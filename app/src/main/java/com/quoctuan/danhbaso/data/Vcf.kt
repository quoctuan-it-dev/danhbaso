package com.quoctuan.danhbaso.data

import java.io.ByteArrayOutputStream

/**
 * Đọc / ghi file danh bạ chuẩn .vcf (vCard).
 * - Ghi: vCard 3.0, UTF-8 – mở được trên iPhone, Google Danh bạ, máy Android khác.
 * - Đọc: hiểu cả vCard 2.1 (Android cũ hay xuất, có QUOTED-PRINTABLE) và 3.0 / 4.0.
 * LƯU Ý: file VCF KHÔNG mã hóa. Sao lưu an toàn thì dùng file .dbat.
 */
object Vcf {

    /** Tạo nội dung file .vcf từ danh sách liên hệ. */
    fun write(list: List<BackupContact>): ByteArray {
        val sb = StringBuilder()
        for (c in list) {
            if (c.phones.isEmpty()) continue
            val name = esc(c.name)
            sb.append("BEGIN:VCARD\r\n")
            sb.append("VERSION:3.0\r\n")
            sb.append("FN:").append(name).append("\r\n")
            sb.append("N:;").append(name).append(";;;\r\n")
            for (p in c.phones) sb.append("TEL;TYPE=CELL:").append(p.filter { it.isDigit() || it == '+' }).append("\r\n")
            sb.append("END:VCARD\r\n")
        }
        return sb.toString().toByteArray(Charsets.UTF_8)
    }

    /** Đọc file .vcf thành danh sách liên hệ (bỏ những thẻ không có số). */
    fun read(bytes: ByteArray): List<BackupContact> {
        val text = bytes.toString(Charsets.UTF_8).replace("\r\n", "\n").replace('\r', '\n')
        // Gộp dòng: dòng bắt đầu bằng khoảng trắng là phần tiếp của dòng trước (vCard 3.0/4.0)
        val raw = text.split('\n')
        val lines = ArrayList<String>()
        var i = 0
        while (i < raw.size) {
            var line = raw[i]
            // QUOTED-PRINTABLE: dòng kết thúc bằng '=' thì nối dòng sau
            if (line.contains("QUOTED-PRINTABLE", ignoreCase = true)) {
                while (line.endsWith("=") && i + 1 < raw.size) {
                    line = line.dropLast(1) + raw[i + 1].trimStart()
                    i++
                }
            }
            if ((line.startsWith(" ") || line.startsWith("\t")) && lines.isNotEmpty()) {
                lines[lines.size - 1] = lines.last() + line.substring(1)
            } else lines += line
            i++
        }

        val out = ArrayList<BackupContact>()
        var fn: String? = null
        var n: String? = null
        var phones = ArrayList<String>()
        for (l in lines) {
            val colon = l.indexOf(':')
            if (colon <= 0) continue
            val head = l.substring(0, colon)
            val value = l.substring(colon + 1)
            val params = head.split(';')
            // Bỏ tiền tố nhóm kiểu "item1.TEL"
            val prop = params[0].substringAfterLast('.').uppercase()
            val qp = params.any { it.contains("QUOTED-PRINTABLE", ignoreCase = true) }
            fun decoded() = unesc(if (qp) decodeQp(value) else value)
            when (prop) {
                "BEGIN" -> { fn = null; n = null; phones = ArrayList() }
                "FN" -> fn = decoded().trim()
                "N" -> {
                    // N: Họ;Tên;Tên đệm;Danh xưng;Hậu tố → ghép theo kiểu Việt: Họ Đệm Tên
                    val p = splitUnescaped(if (qp) decodeQp(value) else value).map { unesc(it).trim() }
                    n = listOfNotNull(p.getOrNull(3), p.getOrNull(0), p.getOrNull(2), p.getOrNull(1))
                        .filter { it.isNotEmpty() }.joinToString(" ")
                }
                "TEL" -> {
                    val num = value.removePrefix("tel:").filter { it.isDigit() || it == '+' || it == '*' || it == '#' }
                    if (num.length >= 3 && phones.none { it == num }) phones += num
                }
                "END" -> {
                    if (phones.isNotEmpty()) {
                        val name = fn?.takeIf { it.isNotBlank() } ?: n?.takeIf { it.isNotBlank() } ?: phones.first()
                        out += BackupContact(name, phones.toList(), false)
                    }
                    fn = null; n = null; phones = ArrayList()
                }
            }
        }
        return out
    }

    private fun esc(s: String) = s.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,").replace("\n", "\\n")

    private fun unesc(s: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '\\' && i + 1 < s.length) {
                val d = s[i + 1]
                sb.append(if (d == 'n' || d == 'N') '\n' else d)
                i += 2
            } else {
                sb.append(c); i++
            }
        }
        return sb.toString()
    }

    /** Tách theo ';' nhưng bỏ qua '\;'. */
    private fun splitUnescaped(s: String): List<String> {
        val parts = ArrayList<String>()
        val cur = StringBuilder()
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '\\' && i + 1 < s.length) { cur.append(c).append(s[i + 1]); i += 2; continue }
            if (c == ';') { parts += cur.toString(); cur.clear() } else cur.append(c)
            i++
        }
        parts += cur.toString()
        return parts
    }

    /** Giải mã QUOTED-PRINTABLE (=E1=BA=B9 → "ẹ"). */
    private fun decodeQp(s: String): String {
        val out = ByteArrayOutputStream()
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '=' && i + 2 < s.length) {
                val v = s.substring(i + 1, i + 3).toIntOrNull(16)
                if (v != null) { out.write(v); i += 3; continue }
            }
            out.write(c.code and 0xFF)
            i++
        }
        return out.toString(Charsets.UTF_8.name())
    }
}
