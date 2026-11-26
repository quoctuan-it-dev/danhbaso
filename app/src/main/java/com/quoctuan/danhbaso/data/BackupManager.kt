package com.quoctuan.danhbaso.data

import android.content.Context
import android.net.Uri
import com.quoctuan.danhbaso.util.PhoneUtil
import org.json.JSONArray
import org.json.JSONObject

/** Tạo file sao lưu và khôi phục danh bạ. */
class BackupManager(
    private val context: Context,
    private val contacts: ContactsRepo,
    private val sim: SimRepo,
) {

    private val maxFileSize = 10 * 1024 * 1024

    /** Danh sách người cần sao lưu (tên + các số + yêu thích). */
    fun collect(all: List<ContactItem>): List<BackupContact> =
        all.map { BackupContact(it.name, it.phones, it.starred) }

    fun toJson(list: List<BackupContact>): ByteArray {
        val arr = JSONArray()
        for (c in list) {
            arr.put(JSONObject().apply {
                put("n", c.name)
                put("p", JSONArray(c.phones))
                put("s", c.starred)
            })
        }
        val root = JSONObject().apply {
            put("app", "danh-ba-so")
            put("v", 1)
            put("created", System.currentTimeMillis())
            put("contacts", arr)
        }
        return root.toString().toByteArray(Charsets.UTF_8)
    }

    fun fromJson(bytes: ByteArray): List<BackupContact> {
        val root = JSONObject(String(bytes, Charsets.UTF_8))
        if (root.optString("app") != "danh-ba-so") throw BackupCrypto.BadFileException()
        val arr = root.getJSONArray("contacts")
        val list = ArrayList<BackupContact>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val p = o.optJSONArray("p") ?: JSONArray()
            val phones = (0 until p.length()).map { p.getString(it) }.filter { it.isNotBlank() }
            if (phones.isEmpty()) continue
            list += BackupContact(o.optString("n"), phones, o.optBoolean("s"))
        }
        return list
    }

    /** Mã hóa và ghi vào file người dùng đã chọn. */
    /** Mã hóa toàn bộ danh bạ thành nội dung file .dbat. Trả về (nội dung, số liên hệ). */
    fun encryptAll(all: List<ContactItem>, password: CharArray): Pair<ByteArray, Int> {
        val list = collect(all)
        val plain = toJson(list)
        try {
            return BackupCrypto.encrypt(plain, password) to list.size
        } finally {
            plain.fill(0)
        }
    }

    /** Ghi nội dung đã mã hóa vào [uri]. */
    fun writeBytes(uri: Uri, bytes: ByteArray) {
        context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(bytes) }
            ?: throw IllegalStateException("Không ghi được file")
    }

    /**
     * Lưu vào Download/DanhBaSo của điện thoại (Android 10 trở lên, không cần hỏi chỗ lưu).
     * Trả về Uri của file, hoặc null nếu máy cũ (khi đó dùng hộp chọn chỗ lưu).
     */
    fun saveToDownloads(name: String, bytes: ByteArray): Uri? {
        if (android.os.Build.VERSION.SDK_INT < 29) return null
        val values = android.content.ContentValues().apply {
            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/octet-stream")
            put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS + "/DanhBaSo")
        }
        val uri = context.contentResolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: throw IllegalStateException("Không tạo được file trong Download")
        writeBytes(uri, bytes)
        return uri
    }

    /** Ghi file tạm (đã mã hóa) để gửi sang app Google Drive. Xóa file tạm cũ trước. */
    fun tempShareFile(name: String, bytes: ByteArray): Uri {
        val dir = java.io.File(context.cacheDir, "share").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val f = java.io.File(dir, name)
        f.writeBytes(bytes)
        return androidx.core.content.FileProvider.getUriForFile(context, context.packageName + ".files", f)
    }

    fun writeBackup(uri: Uri, all: List<ContactItem>, password: CharArray): Int {
        val list = collect(all)
        val plain = toJson(list)
        try {
            val enc = BackupCrypto.encrypt(plain, password)
            context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(enc) }
                ?: throw IllegalStateException("Không ghi được file")
        } finally {
            plain.fill(0)
        }
        return list.size
    }

    /** Đọc + giải mã file sao lưu. */
    fun readBackup(uri: Uri, password: CharArray): List<BackupContact> {
        val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
            val data = input.readBytes()
            if (data.size > maxFileSize) throw BackupCrypto.BadFileException()
            data
        } ?: throw BackupCrypto.BadFileException()
        val plain = BackupCrypto.decrypt(bytes, password)
        try {
            return fromJson(plain)
        } finally {
            plain.fill(0)
        }
    }

    /**
     * Khôi phục cùng lúc vào Gmail ([account], có thể null = không lưu Gmail) và SIM ([toSim]).
     * Số đã có sẵn sẽ được bỏ qua để không tạo bản trùng.
     */
    fun restore(
        list: List<BackupContact>,
        account: String?,
        toSim: Boolean,
        onProgress: (Int, Int) -> Unit,
    ): RestoreResult {
        var gAdded = 0
        var gSkipped = 0
        var gFailed = 0
        var sAdded = 0
        var sSkipped = 0
        var sShort = 0
        val sFailed = ArrayList<String>()

        val accountKeys = account?.let { contacts.numberKeysInAccount(it) } ?: mutableSetOf()
        val simKeys = if (toSim) sim.records().map { PhoneUtil.key(it.number) }.toMutableSet() else mutableSetOf()
        var simFull = false

        list.forEachIndexed { i, c ->
            onProgress(i + 1, list.size)
            val keys = c.phones.map { PhoneUtil.key(it) }
            if (account != null) {
                if (keys.any { it in accountKeys }) {
                    gSkipped++
                } else if (contacts.insertGoogle(account, c.name, c.phones, c.starred)) {
                    gAdded++
                    accountKeys += keys
                } else gFailed++
            }
            if (toSim) {
                val first = c.phones.first()
                val k = PhoneUtil.key(first)
                if (k in simKeys) {
                    sSkipped++
                } else if (simFull) {
                    sFailed += "${c.name} (SIM đầy)"
                } else {
                    when (sim.insert(c.name, first)) {
                        SimWriteResult.OK -> { sAdded++; simKeys += k }
                        SimWriteResult.SHORTENED -> { sAdded++; sShort++; simKeys += k }
                        SimWriteResult.FULL -> { simFull = true; sFailed += "${c.name} (SIM đầy)" }
                        SimWriteResult.NO_SIM -> sFailed += "${c.name} (không thấy SIM)"
                        SimWriteResult.ERROR -> sFailed += "${c.name} (máy không cho ghi)"
                    }
                }
            }
        }
        return RestoreResult(list.size, gAdded, gSkipped, sAdded, sSkipped, sShort, sFailed, gFailed)
    }
}
