package com.quoctuan.danhbaso.data

/** Một người trong danh bạ (gộp các bản ở Gmail, SIM, máy). */
data class ContactItem(
    /** Mã liên hệ của Android. Số âm = liên hệ chỉ có trong SIM (không có trong danh bạ máy). */
    val id: Long,
    val name: String,
    val phones: List<String>,
    val starred: Boolean,
    /** Các tài khoản Gmail đang chứa liên hệ này. */
    val googleAccounts: Set<String>,
    val inSim: Boolean,
) {
    /** Tính sẵn một lần để cuộn danh sách mượt (không tính lại mỗi khung hình). */
    val letter: String = com.quoctuan.danhbaso.util.TextUtil.sectionLetter(name)
    val searchKey: String = com.quoctuan.danhbaso.util.TextUtil.removeDiacritics(name).lowercase()
    val digits: String = phones.joinToString(" ") { com.quoctuan.danhbaso.util.PhoneUtil.local(it) }
    val simOnly: Boolean get() = id < 0
    val firstPhone: String get() = phones.firstOrNull() ?: ""
}

/** Một dòng trong nhật ký cuộc gọi. */
data class CallEntry(
    val number: String,
    val date: Long,
    val type: Int,
    val duration: Long,
)

/** Một số điện thoại trong màn Gần đây: lần gần nhất + tổng số lần. */
data class CallGroup(
    val key: String,
    val number: String,
    val latest: CallEntry,
    val count: Int,
)

data class SimInfo(
    val available: Boolean,
    val maxRecords: Int,
    val used: Int,
    val nameMaxLength: Int,
) {
    val free: Int get() = if (maxRecords > 0) (maxRecords - used).coerceAtLeast(0) else -1

    companion object {
        val NONE = SimInfo(false, 0, 0, 0)
    }
}

data class SimRecord(val name: String, val number: String, val recordNumber: Int)

enum class SimWriteResult { OK, SHORTENED, FULL, ERROR, NO_SIM }

/** Liên hệ trong file sao lưu. */
data class BackupContact(val name: String, val phones: List<String>, val starred: Boolean)

data class RestoreResult(
    val total: Int,
    val googleAdded: Int,
    val googleSkipped: Int,
    val simAdded: Int,
    val simSkipped: Int,
    val simShortened: Int,
    val simFailed: List<String>,
    val googleFailed: Int,
)
