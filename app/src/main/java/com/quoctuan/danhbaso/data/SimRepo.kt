package com.quoctuan.danhbaso.data

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import android.os.Build
import android.provider.SimPhonebookContract
import android.telephony.SubscriptionManager
import androidx.annotation.RequiresApi
import com.quoctuan.danhbaso.util.PhoneUtil
import com.quoctuan.danhbaso.util.TextUtil

/**
 * Đọc / ghi số trong SIM.
 * Android 12 trở lên dùng SimPhonebookContract (chuẩn chính thức).
 * Máy cũ hơn dùng content://icc/adn (tùy hãng, có máy không hỗ trợ).
 * Lưu ý: SIM chỉ chứa khoảng 200–250 số và tên ngắn (~14 ký tự).
 */
class SimRepo(private val context: Context) {

    private val cr get() = context.contentResolver
    private val legacyUri: Uri = "content://icc/adn".toUri()

    /** Mã SIM đang dùng (SIM 1). */
    @SuppressLint("MissingPermission")
    private fun subId(): Int? {
        return try {
            val sm = context.getSystemService(SubscriptionManager::class.java) ?: return null
            sm.activeSubscriptionInfoList?.firstOrNull()?.subscriptionId
        } catch (e: Exception) {
            null
        }
    }

    fun info(): SimInfo {
        if (Build.VERSION.SDK_INT >= 31) {
            val id = subId() ?: return SimInfo.NONE
            return try {
                infoV31(id)
            } catch (e: Exception) {
                SimInfo.NONE
            }
        }
        return try {
            SimInfo(true, 0, recordsLegacy().size, 14)
        } catch (e: Exception) {
            SimInfo.NONE
        }
    }

    @RequiresApi(31)
    private fun infoV31(id: Int): SimInfo {
        val uri = SimPhonebookContract.ElementaryFiles.getItemUri(id, SimPhonebookContract.ElementaryFiles.EF_ADN)
        cr.query(
            uri, arrayOf(
                SimPhonebookContract.ElementaryFiles.MAX_RECORDS,
                SimPhonebookContract.ElementaryFiles.RECORD_COUNT,
                SimPhonebookContract.ElementaryFiles.NAME_MAX_LENGTH,
            ), null, null
        )?.use { c ->
            if (c.moveToFirst()) return SimInfo(true, c.getInt(0), c.getInt(1), c.getInt(2))
        }
        return SimInfo.NONE
    }

    fun records(): List<SimRecord> {
        return try {
            if (Build.VERSION.SDK_INT >= 31) {
                val id = subId() ?: return emptyList()
                recordsV31(id)
            } else recordsLegacy()
        } catch (e: Exception) {
            emptyList()
        }
    }

    @RequiresApi(31)
    private fun recordsV31(id: Int): List<SimRecord> {
        val list = ArrayList<SimRecord>()
        val uri = SimPhonebookContract.SimRecords.getContentUri(id, SimPhonebookContract.ElementaryFiles.EF_ADN)
        cr.query(
            uri, arrayOf(
                SimPhonebookContract.SimRecords.NAME,
                SimPhonebookContract.SimRecords.PHONE_NUMBER,
                SimPhonebookContract.SimRecords.RECORD_NUMBER,
            ), null, null
        )?.use { c ->
            while (c.moveToNext()) {
                list += SimRecord(c.getString(0) ?: "", c.getString(1) ?: "", c.getInt(2))
            }
        }
        return list
    }

    private fun recordsLegacy(): List<SimRecord> {
        val list = ArrayList<SimRecord>()
        cr.query(legacyUri, null, null, null, null)?.use { c ->
            val iName = c.getColumnIndex("name")
            val iNum = c.getColumnIndex("number")
            if (iNum < 0) return list
            while (c.moveToNext()) {
                list += SimRecord(if (iName >= 0) c.getString(iName) ?: "" else "", c.getString(iNum) ?: "", -1)
            }
        }
        return list
    }

    /** Số byte tối đa của ô tên trong SIM đang lắp (thường 14). */
    fun nameLimit(): Int = info().nameMaxLength.takeIf { it > 0 } ?: 14

    /** Số byte tên [s] chiếm trong SIM (có dấu = khoảng 2 byte mỗi chữ). */
    fun encodedLength(s: String): Int {
        if (Build.VERSION.SDK_INT >= 31) {
            try { return SimPhonebookContract.SimRecords.getEncodedNameLength(cr, s) } catch (_: Exception) { }
        }
        // Ước lượng: toàn chữ không dấu = 1 byte/chữ; có chữ có dấu = 1 + 2 byte/chữ (UCS-2)
        return if (s.all { it.code < 128 }) s.length else 1 + s.length * 2
    }

    /**
     * Tên sẽ lưu vào SIM.
     * [keepAccents] = true: giữ dấu, cắt bớt cho vừa; false: bỏ dấu rồi cắt (chứa được gấp đôi chữ).
     */
    fun previewName(name: String, keepAccents: Boolean): String {
        val limit = nameLimit()
        var n = (if (keepAccents) name else TextUtil.removeDiacritics(name)).trim()
        while (n.isNotEmpty() && encodedLength(n) > limit) n = n.dropLast(1)
        return n.trim()
    }

    /** Ghi một số vào SIM. Tên quá dài sẽ được bỏ dấu / rút gọn. */
    fun insert(name: String, number: String): SimWriteResult {
        val num = PhoneUtil.local(number)
        if (num.isEmpty()) return SimWriteResult.ERROR
        if (Build.VERSION.SDK_INT >= 31) {
            val id = subId() ?: return SimWriteResult.NO_SIM
            return try {
                insertV31(id, name, num)
            } catch (e: Exception) {
                SimWriteResult.ERROR
            }
        }
        return try {
            val fitted = if (encodedLength(name) <= 14) name else TextUtil.removeDiacritics(name).take(14).trim()
            val v = ContentValues().apply {
                put("tag", fitted)
                put("number", num)
            }
            if (cr.insert(legacyUri, v) != null) {
                if (fitted != name) SimWriteResult.SHORTENED else SimWriteResult.OK
            } else SimWriteResult.ERROR
        } catch (e: Exception) {
            SimWriteResult.ERROR
        }
    }

    @RequiresApi(31)
    private fun insertV31(id: Int, name: String, number: String): SimWriteResult {
        val info = infoV31(id)
        if (info.maxRecords > 0 && info.used >= info.maxRecords) return SimWriteResult.FULL
        val fitted = fitName(name, info.nameMaxLength)
        val v = ContentValues().apply {
            put(SimPhonebookContract.SimRecords.NAME, fitted)
            put(SimPhonebookContract.SimRecords.PHONE_NUMBER, number)
        }
        val uri = SimPhonebookContract.SimRecords.getContentUri(id, SimPhonebookContract.ElementaryFiles.EF_ADN)
        val res = cr.insert(uri, v) ?: return SimWriteResult.ERROR
        return if (res.toString().isNotEmpty() && fitted != name) SimWriteResult.SHORTENED else SimWriteResult.OK
    }

    @RequiresApi(31)
    private fun fitName(name: String, maxLen: Int): String {
        fun len(s: String): Int = try {
            SimPhonebookContract.SimRecords.getEncodedNameLength(cr, s)
        } catch (e: Exception) {
            s.length * 2
        }
        val limit = if (maxLen > 0) maxLen else 14
        if (len(name) <= limit) return name
        var n = TextUtil.removeDiacritics(name)
        while (n.isNotEmpty() && len(n) > limit) n = n.dropLast(1)
        return n.trim()
    }

    /** Xóa (các) bản ghi trong SIM có số này. */
    fun deleteNumber(number: String): Boolean {
        val key = PhoneUtil.key(number)
        return try {
            if (Build.VERSION.SDK_INT >= 31) {
                val id = subId() ?: return false
                var ok = false
                for (r in recordsV31(id)) {
                    if (PhoneUtil.key(r.number) == key) {
                        val uri = SimPhonebookContract.SimRecords.getItemUri(
                            id, SimPhonebookContract.ElementaryFiles.EF_ADN, r.recordNumber
                        )
                        if (cr.delete(uri, null, null) > 0) ok = true
                    }
                }
                ok
            } else {
                var ok = false
                for (r in recordsLegacy()) {
                    if (PhoneUtil.key(r.number) == key) {
                        val where = "tag='${r.name.replace("'", "")}' AND number='${r.number.replace("'", "")}'"
                        if (cr.delete(legacyUri, where, null) > 0) ok = true
                    }
                }
                ok
            }
        } catch (e: Exception) {
            false
        }
    }
}
