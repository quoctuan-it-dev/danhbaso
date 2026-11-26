package com.quoctuan.danhbaso.data

import android.accounts.AccountManager
import android.content.ContentProviderOperation
import android.content.ContentValues
import android.content.Context
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.CommonDataKinds.StructuredName
import android.provider.ContactsContract.Data
import android.provider.ContactsContract.RawContacts
import com.quoctuan.danhbaso.util.PhoneUtil
import java.text.Collator
import java.util.Locale

/**
 * Đọc / ghi danh bạ dùng chung của Android.
 * App Điện thoại gốc và app này dùng CÙNG kho dữ liệu, nên thay đổi ở đây sẽ hiện ở cả hai.
 */
class ContactsRepo(private val context: Context) {

    private val cr get() = context.contentResolver

    companion object {
        const val GOOGLE = "com.google"
        private const val COL_ACCOUNT_TYPE = "account_type"
        private const val COL_ACCOUNT_NAME = "account_name"
    }

    private class Builder(val id: Long, var name: String, var starred: Boolean) {
        val phones = LinkedHashMap<String, String>() // key -> số gốc
        val accounts = HashSet<String>()
        var inSimAccount = false
    }

    /**
     * Đọc toàn bộ người có số điện thoại.
     * [simRecords]: các số đang có trong SIM, dùng để đánh dấu "SIM" và thêm người chỉ có trong SIM.
     */
    fun loadAll(simRecords: List<SimRecord>): List<ContactItem> {
        val map = LinkedHashMap<Long, Builder>()
        val projection = arrayOf(
            Phone.CONTACT_ID,
            Phone.DISPLAY_NAME_PRIMARY,
            Phone.NUMBER,
            Phone.STARRED,
            COL_ACCOUNT_TYPE,
            COL_ACCOUNT_NAME,
        )
        try {
            cr.query(Phone.CONTENT_URI, projection, null, null, null)?.use { c ->
                while (c.moveToNext()) {
                    val id = c.getLong(0)
                    val name = c.getString(1) ?: ""
                    val number = c.getString(2) ?: continue
                    val starred = c.getInt(3) == 1
                    val type = c.getString(4) ?: ""
                    val accName = c.getString(5) ?: ""
                    val b = map.getOrPut(id) { Builder(id, name, starred) }
                    if (b.name.isBlank()) b.name = name
                    b.starred = b.starred || starred
                    val k = PhoneUtil.key(number)
                    if (k.isNotEmpty() && !b.phones.containsKey(k)) b.phones[k] = number
                    if (type == GOOGLE && accName.isNotEmpty()) b.accounts += accName
                    if (type.contains("sim", ignoreCase = true)) b.inSimAccount = true
                }
            }
        } catch (e: SecurityException) {
            return emptyList()
        }

        val simKeys = simRecords.map { PhoneUtil.key(it.number) }.toSet()
        val result = ArrayList<ContactItem>()
        val knownKeys = HashSet<String>()
        for (b in map.values) {
            if (b.phones.isEmpty()) continue
            knownKeys += b.phones.keys
            result += ContactItem(
                id = b.id,
                name = b.name.ifBlank { PhoneUtil.pretty(b.phones.values.first()) },
                phones = b.phones.values.toList(),
                starred = b.starred,
                googleAccounts = b.accounts,
                inSim = b.inSimAccount || b.phones.keys.any { it in simKeys },
            )
        }
        // Người chỉ có trong SIM mà danh bạ máy chưa có
        var fakeId = -1L
        for (r in simRecords) {
            val k = PhoneUtil.key(r.number)
            if (k.isEmpty() || k in knownKeys) continue
            knownKeys += k
            result += ContactItem(
                id = fakeId--,
                name = r.name.ifBlank { PhoneUtil.pretty(r.number) },
                phones = listOf(r.number),
                starred = false,
                googleAccounts = emptySet(),
                inSim = true,
            )
        }
        val collator = Collator.getInstance(Locale("vi", "VN"))
        return result.sortedWith { a, b -> collator.compare(a.name, b.name) }
    }

    /** Các tài khoản Gmail đang đăng nhập trên máy. */
    fun googleAccounts(): List<String> {
        val set = LinkedHashSet<String>()
        try {
            AccountManager.get(context).getAccountsByType(GOOGLE).forEach { set += it.name }
        } catch (e: Exception) {
        }
        try {
            cr.query(
                RawContacts.CONTENT_URI, arrayOf(COL_ACCOUNT_NAME),
                "$COL_ACCOUNT_TYPE=?", arrayOf(GOOGLE), null
            )?.use { c -> while (c.moveToNext()) c.getString(0)?.let { set += it } }
        } catch (e: Exception) {
        }
        return set.toList()
    }

    /** Thêm một người mới vào tài khoản Gmail. */
    fun insertGoogle(account: String, name: String, phones: List<String>, starred: Boolean): Boolean {
        if (phones.isEmpty()) return false
        val ops = ArrayList<ContentProviderOperation>()
        val raw = ContentProviderOperation.newInsert(RawContacts.CONTENT_URI)
            .withValue(RawContacts.ACCOUNT_TYPE, GOOGLE)
            .withValue(RawContacts.ACCOUNT_NAME, account)
        if (starred) raw.withValue(RawContacts.STARRED, 1)
        ops += raw.build()
        ops += ContentProviderOperation.newInsert(Data.CONTENT_URI)
            .withValueBackReference(Data.RAW_CONTACT_ID, 0)
            .withValue(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE)
            .withValue(StructuredName.DISPLAY_NAME, name)
            .build()
        for (p in phones) {
            ops += ContentProviderOperation.newInsert(Data.CONTENT_URI)
                .withValueBackReference(Data.RAW_CONTACT_ID, 0)
                .withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
                .withValue(Phone.NUMBER, p)
                .withValue(Phone.TYPE, Phone.TYPE_MOBILE)
                .build()
        }
        return try {
            cr.applyBatch(ContactsContract.AUTHORITY, ops)
            true
        } catch (e: Exception) {
            false
        }
    }

    /** Các số (khóa 9 số cuối) đang có trong một tài khoản Gmail, để bỏ qua số trùng. */
    fun numberKeysInAccount(account: String): MutableSet<String> {
        val set = HashSet<String>()
        try {
            cr.query(
                Phone.CONTENT_URI, arrayOf(Phone.NUMBER),
                "$COL_ACCOUNT_TYPE=? AND $COL_ACCOUNT_NAME=?", arrayOf(GOOGLE, account), null
            )?.use { c -> while (c.moveToNext()) c.getString(0)?.let { set += PhoneUtil.key(it) } }
        } catch (e: Exception) {
        }
        return set
    }

    /** Đếm số người trong mỗi tài khoản Gmail. */
    fun countByAccount(): Map<String, Int> {
        val map = HashMap<String, MutableSet<Long>>()
        try {
            cr.query(
                RawContacts.CONTENT_URI, arrayOf(COL_ACCOUNT_NAME, RawContacts.CONTACT_ID),
                "$COL_ACCOUNT_TYPE=? AND ${RawContacts.DELETED}=0", arrayOf(GOOGLE), null
            )?.use { c ->
                while (c.moveToNext()) {
                    val n = c.getString(0) ?: continue
                    map.getOrPut(n) { HashSet() } += c.getLong(1)
                }
            }
        } catch (e: Exception) {
        }
        return map.mapValues { it.value.size }
    }

    fun setStarred(contactId: Long, starred: Boolean) {
        val v = ContentValues().apply { put(ContactsContract.Contacts.STARRED, if (starred) 1 else 0) }
        try {
            cr.update(ContactsContract.Contacts.CONTENT_URI, v, "${ContactsContract.Contacts._ID}=?", arrayOf(contactId.toString()))
        } catch (e: Exception) {
        }
    }

    /** Xóa hẳn người này khỏi máy và Gmail (Gmail sẽ tự đồng bộ xóa). */
    fun deleteContact(contactId: Long): Boolean = try {
        cr.delete(RawContacts.CONTENT_URI, "${RawContacts.CONTACT_ID}=?", arrayOf(contactId.toString())) > 0
    } catch (e: Exception) {
        false
    }

    /** Xóa riêng bản trong một tài khoản Gmail. */
    fun deleteFromAccount(contactId: Long, account: String): Boolean = try {
        cr.delete(
            RawContacts.CONTENT_URI,
            "${RawContacts.CONTACT_ID}=? AND $COL_ACCOUNT_TYPE=? AND $COL_ACCOUNT_NAME=?",
            arrayOf(contactId.toString(), GOOGLE, account)
        ) > 0
    } catch (e: Exception) {
        false
    }

    /** Đổi tên và số (số cũ [oldNumber] được thay bằng [newNumber]) ở mọi bản của người này. */
    /** Xóa một số điện thoại khỏi liên hệ (mọi bản ghi Gmail / máy của người đó). */
    fun removePhone(contactId: Long, number: String): Boolean = try {
        val key = PhoneUtil.key(number)
        val ids = ArrayList<Long>()
        cr.query(
            Data.CONTENT_URI, arrayOf(Data._ID, Phone.NUMBER),
            "${Data.CONTACT_ID}=? AND ${Data.MIMETYPE}=?",
            arrayOf(contactId.toString(), Phone.CONTENT_ITEM_TYPE), null
        )?.use { c -> while (c.moveToNext()) if (PhoneUtil.key(c.getString(1) ?: "") == key) ids += c.getLong(0) }
        var n = 0
        for (id in ids) n += cr.delete(Data.CONTENT_URI, "${Data._ID}=?", arrayOf(id.toString()))
        n > 0
    } catch (e: Exception) {
        false
    }

    fun updateNameAndNumber(contactId: Long, newName: String, oldNumber: String, newNumber: String): Boolean {
        return try {
            val rawIds = ArrayList<Long>()
            cr.query(
                RawContacts.CONTENT_URI, arrayOf(RawContacts._ID),
                "${RawContacts.CONTACT_ID}=?", arrayOf(contactId.toString()), null
            )?.use { c -> while (c.moveToNext()) rawIds += c.getLong(0) }
            val ops = ArrayList<ContentProviderOperation>()
            val oldKey = PhoneUtil.key(oldNumber)
            for (rid in rawIds) {
                // Tên
                var hasName = false
                cr.query(
                    Data.CONTENT_URI, arrayOf(Data._ID),
                    "${Data.RAW_CONTACT_ID}=? AND ${Data.MIMETYPE}=?",
                    arrayOf(rid.toString(), StructuredName.CONTENT_ITEM_TYPE), null
                )?.use { c ->
                    while (c.moveToNext()) {
                        hasName = true
                        ops += ContentProviderOperation.newUpdate(Data.CONTENT_URI)
                            .withSelection("${Data._ID}=?", arrayOf(c.getLong(0).toString()))
                            .withValue(StructuredName.DISPLAY_NAME, newName)
                            .withValue(StructuredName.GIVEN_NAME, null)
                            .withValue(StructuredName.FAMILY_NAME, null)
                            .withValue(StructuredName.MIDDLE_NAME, null)
                            .withValue(StructuredName.PREFIX, null)
                            .withValue(StructuredName.SUFFIX, null)
                            .build()
                    }
                }
                if (!hasName) {
                    ops += ContentProviderOperation.newInsert(Data.CONTENT_URI)
                        .withValue(Data.RAW_CONTACT_ID, rid)
                        .withValue(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE)
                        .withValue(StructuredName.DISPLAY_NAME, newName)
                        .build()
                }
                // Số
                if (PhoneUtil.key(newNumber) != oldKey) {
                    cr.query(
                        Data.CONTENT_URI, arrayOf(Data._ID, Phone.NUMBER),
                        "${Data.RAW_CONTACT_ID}=? AND ${Data.MIMETYPE}=?",
                        arrayOf(rid.toString(), Phone.CONTENT_ITEM_TYPE), null
                    )?.use { c ->
                        while (c.moveToNext()) {
                            if (PhoneUtil.key(c.getString(1) ?: "") == oldKey) {
                                ops += ContentProviderOperation.newUpdate(Data.CONTENT_URI)
                                    .withSelection("${Data._ID}=?", arrayOf(c.getLong(0).toString()))
                                    .withValue(Phone.NUMBER, newNumber)
                                    .build()
                            }
                        }
                    }
                }
            }
            if (ops.isNotEmpty()) cr.applyBatch(ContactsContract.AUTHORITY, ops)
            true
        } catch (e: Exception) {
            false
        }
    }

    /** "Thêm vào người có sẵn": gắn thêm một số vào người đã có. */
    fun addPhoneToContact(contactId: Long, number: String, preferAccount: String?): Boolean {
        return try {
            var rawId: Long? = null
            var fallback: Long? = null
            cr.query(
                RawContacts.CONTENT_URI, arrayOf(RawContacts._ID, COL_ACCOUNT_TYPE, COL_ACCOUNT_NAME),
                "${RawContacts.CONTACT_ID}=?", arrayOf(contactId.toString()), null
            )?.use { c ->
                while (c.moveToNext()) {
                    if (fallback == null) fallback = c.getLong(0)
                    if (c.getString(1) == GOOGLE && c.getString(2) == preferAccount) rawId = c.getLong(0)
                }
            }
            val target = rawId ?: fallback ?: return false
            val v = ContentValues().apply {
                put(Data.RAW_CONTACT_ID, target)
                put(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
                put(Phone.NUMBER, number)
                put(Phone.TYPE, Phone.TYPE_MOBILE)
            }
            cr.insert(Data.CONTENT_URI, v) != null
        } catch (e: Exception) {
            false
        }
    }
}
