package com.quoctuan.danhbaso.util

import java.text.Normalizer

/** Các hàm xử lý số điện thoại và chữ tiếng Việt. */
object PhoneUtil {

    /** Chỉ giữ chữ số, đổi đầu 84 thành 0. Ví dụ "+84 912 345 678" -> "0912345678". */
    fun local(raw: String): String {
        val d = raw.filter { it.isDigit() }
        return if (d.startsWith("84") && d.length >= 11) "0" + d.substring(2) else d
    }

    /** Khóa so sánh: 9 số cuối, để "0912..." và "+84912..." được coi là cùng một số. */
    fun key(raw: String): String {
        val d = local(raw)
        return if (d.length > 9) d.takeLast(9) else d
    }

    /** Hiển thị đẹp: 0912 345 678 hoặc 028 9999 5555. */
    fun pretty(raw: String): String {
        val d = local(raw)
        return when {
            d.length == 10 -> "${d.substring(0, 4)} ${d.substring(4, 7)} ${d.substring(7)}"
            d.length == 11 -> "${d.substring(0, 3)} ${d.substring(3, 7)} ${d.substring(7)}"
            d.isEmpty() -> raw.ifBlank { "Số ẩn" }
            else -> d
        }
    }
}

object TextUtil {
    private val MARKS = Regex("\\p{Mn}+")

    /** Bỏ dấu tiếng Việt: "Điện Máy" -> "Dien May". */
    fun removeDiacritics(s: String): String {
        val n = Normalizer.normalize(s, Normalizer.Form.NFD)
        return n.replace(MARKS, "").replace('đ', 'd').replace('Đ', 'D')
    }

    /** Chữ cái nhóm trong danh bạ: "Ánh" -> "A", "Điện" -> "Đ". */
    fun sectionLetter(name: String): String {
        val c = name.trim().firstOrNull() ?: return "#"
        if (c == 'Đ' || c == 'đ') return "Đ"
        if (!c.isLetter()) return "#"
        return removeDiacritics(c.toString()).uppercase()
    }

    /** Tìm kiếm không phân biệt dấu và hoa thường. */
    fun matches(text: String, query: String): Boolean =
        removeDiacritics(text).lowercase().contains(removeDiacritics(query).lowercase())
}
