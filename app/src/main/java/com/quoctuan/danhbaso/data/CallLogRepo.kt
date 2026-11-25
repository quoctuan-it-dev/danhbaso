package com.quoctuan.danhbaso.data

import android.content.Context
import android.provider.CallLog
import com.quoctuan.danhbaso.util.PhoneUtil

/** Đọc nhật ký cuộc gọi dùng chung của Android (cùng dữ liệu với app Điện thoại gốc). */
class CallLogRepo(private val context: Context) {

    fun load(limit: Int = 1000): List<CallEntry> {
        val result = ArrayList<CallEntry>()
        val projection = arrayOf(
            CallLog.Calls.NUMBER,
            CallLog.Calls.DATE,
            CallLog.Calls.TYPE,
            CallLog.Calls.DURATION,
        )
        try {
            context.contentResolver.query(
                CallLog.Calls.CONTENT_URI, projection, null, null,
                "${CallLog.Calls.DATE} DESC"
            )?.use { c ->
                while (c.moveToNext() && result.size < limit) {
                    result += CallEntry(
                        number = c.getString(0) ?: "",
                        date = c.getLong(1),
                        type = c.getInt(2),
                        duration = c.getLong(3),
                    )
                }
            }
        } catch (e: SecurityException) {
            // Chưa có quyền nhật ký cuộc gọi
        }
        return result
    }

    companion object {
        /** Gộp theo số: mỗi số chỉ 1 dòng là lần gần nhất, kèm tổng số lần. */
        fun group(calls: List<CallEntry>): List<CallGroup> {
            val map = LinkedHashMap<String, CallGroup>()
            for (e in calls) {
                val k = PhoneUtil.key(e.number).ifEmpty { "hidden" }
                val g = map[k]
                map[k] = if (g == null) CallGroup(k, e.number, e, 1) else g.copy(count = g.count + 1)
            }
            return map.values.toList()
        }
    }
}
