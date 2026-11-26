package com.quoctuan.danhbaso.data

import android.content.ContentValues
import android.content.Context
import android.provider.ContactsContract.CommonDataKinds.GroupMembership
import android.provider.ContactsContract.Data
import android.provider.ContactsContract.Groups
import android.provider.ContactsContract.RawContacts

/** Một nhóm liên hệ (chính là "Nhãn" trong Google Danh bạ). */
data class GroupInfo(val id: Long, val title: String)

/**
 * Nhóm liên hệ theo tài khoản Gmail.
 * Dùng bảng Groups chuẩn của Android, nên Google tự đồng bộ thành "Nhãn" trong Google Danh bạ.
 * Chỉ hiện nhóm do người dùng tạo (bỏ qua nhóm hệ thống như "My Contacts", "Starred").
 */
class GroupsRepo(private val context: Context) {

    private val cr get() = context.contentResolver

    /** Các nhóm của tài khoản Gmail [account], xếp theo tên. */
    fun groups(account: String): List<GroupInfo> = try {
        val list = ArrayList<GroupInfo>()
        cr.query(
            Groups.CONTENT_URI, arrayOf(Groups._ID, Groups.TITLE),
            "${Groups.ACCOUNT_TYPE}=? AND ${Groups.ACCOUNT_NAME}=? AND ${Groups.DELETED}=0 AND ${Groups.SYSTEM_ID} IS NULL" +
                " AND (${Groups.GROUP_IS_READ_ONLY} IS NULL OR ${Groups.GROUP_IS_READ_ONLY}=0)",
            arrayOf(ContactsRepo.GOOGLE, account), null
        )?.use { c ->
            while (c.moveToNext()) {
                val title = c.getString(1)?.trim().orEmpty()
                // Bỏ nhóm Google tự tạo cho Yêu thích ("Starred in Android")
                if (title.isNotEmpty() && !title.equals("Starred in Android", ignoreCase = true)) list += GroupInfo(c.getLong(0), title)
            }
        }
        val collator = java.text.Collator.getInstance(java.util.Locale("vi"))
        list.sortedWith { a, b -> collator.compare(a.title, b.title) }
    } catch (e: Exception) {
        emptyList()
    }

    /** Mã nhóm → tập mã liên hệ (contact id) trong nhóm đó. */
    fun members(groupIds: Collection<Long>): Map<Long, Set<Long>> {
        if (groupIds.isEmpty()) return emptyMap()
        val map = HashMap<Long, HashSet<Long>>()
        try {
            val marks = groupIds.joinToString(",") { "?" }
            cr.query(
                Data.CONTENT_URI, arrayOf(GroupMembership.GROUP_ROW_ID, Data.CONTACT_ID),
                "${Data.MIMETYPE}=? AND ${GroupMembership.GROUP_ROW_ID} IN ($marks)",
                arrayOf(GroupMembership.CONTENT_ITEM_TYPE) + groupIds.map { it.toString() }, null
            )?.use { c ->
                while (c.moveToNext()) map.getOrPut(c.getLong(0)) { HashSet() } += c.getLong(1)
            }
        } catch (_: Exception) {
        }
        return map
    }

    /** Tạo nhóm mới trong Gmail [account]. Trả về mã nhóm, hoặc null nếu lỗi. */
    fun create(account: String, title: String): Long? = try {
        val v = ContentValues().apply {
            put(Groups.ACCOUNT_TYPE, ContactsRepo.GOOGLE)
            put(Groups.ACCOUNT_NAME, account)
            put(Groups.TITLE, title)
            put(Groups.GROUP_VISIBLE, 1)
            put(Groups.SHOULD_SYNC, 1)
        }
        cr.insert(Groups.CONTENT_URI, v)?.lastPathSegment?.toLongOrNull()
    } catch (e: Exception) {
        null
    }

    /** Đổi tên nhóm. */
    fun rename(groupId: Long, title: String): Boolean = try {
        val v = ContentValues().apply { put(Groups.TITLE, title) }
        cr.update(android.content.ContentUris.withAppendedId(Groups.CONTENT_URI, groupId), v, null, null) > 0
    } catch (e: Exception) {
        false
    }

    /** Xóa nhóm. Người trong nhóm KHÔNG bị xóa, chỉ mất nhãn nhóm. */
    fun delete(groupId: Long): Boolean = try {
        cr.delete(android.content.ContentUris.withAppendedId(Groups.CONTENT_URI, groupId), null, null) > 0
    } catch (e: Exception) {
        false
    }

    /** Các bản ghi (raw contact) của liên hệ nằm trong Gmail [account]. */
    private fun rawIds(contactId: Long, account: String): List<Long> {
        val ids = ArrayList<Long>()
        cr.query(
            RawContacts.CONTENT_URI, arrayOf(RawContacts._ID),
            "${RawContacts.CONTACT_ID}=? AND ${RawContacts.ACCOUNT_TYPE}=? AND ${RawContacts.ACCOUNT_NAME}=? AND ${RawContacts.DELETED}=0",
            arrayOf(contactId.toString(), ContactsRepo.GOOGLE, account), null
        )?.use { c -> while (c.moveToNext()) ids += c.getLong(0) }
        return ids
    }

    /** Cho liên hệ vào nhóm. */
    fun addMember(contactId: Long, groupId: Long, account: String): Boolean = try {
        val raw = rawIds(contactId, account).firstOrNull()
        if (raw == null) false else {
            val v = ContentValues().apply {
                put(Data.RAW_CONTACT_ID, raw)
                put(Data.MIMETYPE, GroupMembership.CONTENT_ITEM_TYPE)
                put(GroupMembership.GROUP_ROW_ID, groupId)
            }
            cr.insert(Data.CONTENT_URI, v) != null
        }
    } catch (e: Exception) {
        false
    }

    /** Bỏ liên hệ ra khỏi nhóm. */
    fun removeMember(contactId: Long, groupId: Long, account: String): Boolean = try {
        val raws = rawIds(contactId, account)
        var n = 0
        for (r in raws) {
            n += cr.delete(
                Data.CONTENT_URI,
                "${Data.RAW_CONTACT_ID}=? AND ${Data.MIMETYPE}=? AND ${GroupMembership.GROUP_ROW_ID}=?",
                arrayOf(r.toString(), GroupMembership.CONTENT_ITEM_TYPE, groupId.toString())
            )
        }
        n > 0
    } catch (e: Exception) {
        false
    }
}
