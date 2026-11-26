package com.quoctuan.danhbaso.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.quoctuan.danhbaso.util.PhoneUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Dữ liệu dùng chung cho mọi màn hình. Gọi [refresh] để đọc lại từ điện thoại. */
class AppData(context: Context) {
    val prefs = Prefs(context)
    val contactsRepo = ContactsRepo(context)
    val callRepo = CallLogRepo(context)
    val simRepo = SimRepo(context)
    val backup = BackupManager(context, contactsRepo, simRepo)
    val groupsRepo = GroupsRepo(context)

    var contacts by mutableStateOf<List<ContactItem>>(emptyList())
        private set
    var calls by mutableStateOf<List<CallEntry>>(emptyList())
        private set
    var accounts by mutableStateOf<List<String>>(emptyList())
        private set
    var accountCounts by mutableStateOf<Map<String, Int>>(emptyMap())
        private set
    var selectedAccount by mutableStateOf<String?>(null)
        private set
    var simInfo by mutableStateOf(SimInfo.NONE)
        private set
    var lastBackup by mutableStateOf(0L)
        private set
    var loaded by mutableStateOf(false)
        private set
    var keypadSize by mutableStateOf(prefs.keypadSize)
        private set

    var theme by mutableStateOf(prefs.theme.coerceIn(0, 3))
        private set

    /** Cỡ chữ toàn app: 0 Nhỏ, 1 Vừa, 2 Lớn, 3 Rất lớn. */
    var fontSize by mutableStateOf(prefs.fontSize.coerceIn(0, 3))
        private set
    /** Hệ số phóng chữ tương ứng với [fontSize]. */
    val fontFactor: Float get() = FONT_FACTORS[fontSize]

    fun changeFontSize(v: Int) {
        fontSize = v.coerceIn(0, 3)
        prefs.fontSize = fontSize
    }

    /** Nhóm của Gmail đang chọn, và thành viên của từng nhóm (mã nhóm → mã liên hệ). */
    var groups by mutableStateOf<List<GroupInfo>>(emptyList())
        private set
    var groupMembers by mutableStateOf<Map<Long, Set<Long>>>(emptyMap())
        private set

    /** Đọc lại nhóm (gọi sau khi tạo nhóm / thêm, bỏ người khỏi nhóm). */
    suspend fun reloadGroups() {
        val acc = selectedAccount
        val (g, m) = withContext(Dispatchers.IO) {
            if (acc == null) emptyList<GroupInfo>() to emptyMap<Long, Set<Long>>()
            else {
                val list = groupsRepo.groups(acc)
                list to groupsRepo.members(list.map { it.id })
            }
        }
        groups = g
        groupMembers = m
    }

    /** Các nhóm mà liên hệ [contactId] đang thuộc. */
    fun groupsOf(contactId: Long): List<GroupInfo> = groups.filter { contactId in (groupMembers[it.id] ?: emptySet()) }

    fun changeTheme(v: Int) {
        theme = v.coerceIn(0, 3)
        prefs.theme = theme
    }

    fun changeKeypadSize(v: Int) {
        keypadSize = v.coerceIn(0, 2)
        prefs.keypadSize = keypadSize
    }

    private var byKey: Map<String, ContactItem> = emptyMap()

    suspend fun refresh() {
        val acc = withContext(Dispatchers.IO) { contactsRepo.googleAccounts() }
        val counts = withContext(Dispatchers.IO) { contactsRepo.countByAccount() }
        val simRecords = withContext(Dispatchers.IO) { simRepo.records() }
        val sInfo = withContext(Dispatchers.IO) { simRepo.info() }
        val all = withContext(Dispatchers.IO) { contactsRepo.loadAll(simRecords) }
        val log = withContext(Dispatchers.IO) { callRepo.load() }

        val keyMap = HashMap<String, ContactItem>()
        for (c in all) for (p in c.phones) keyMap.putIfAbsent(PhoneUtil.key(p), c)

        accounts = acc
        accountCounts = counts
        selectedAccount = prefs.account?.takeIf { it in acc } ?: acc.firstOrNull()
        simInfo = sInfo
        byKey = keyMap
        contacts = all
        calls = log
        lastBackup = prefs.lastBackup
        updateVisible()
        reloadGroups()
        loaded = true
    }

    fun selectAccount(name: String) {
        selectedAccount = name
        prefs.account = name
        updateVisible()
    }

    fun markBackup(name: String) {
        val now = System.currentTimeMillis()
        prefs.lastBackup = now
        prefs.lastBackupName = name
        lastBackup = now
    }

    /** Danh bạ đang xem: người trong Gmail đang chọn + SIM + máy (ẩn người chỉ nằm ở Gmail khác). */
    var visibleContacts by mutableStateOf<List<ContactItem>>(emptyList())
        private set

    private fun updateVisible() {
        val sel = selectedAccount
        visibleContacts = if (sel == null) contacts
        else contacts.filter { it.googleAccounts.isEmpty() || sel in it.googleAccounts }
    }

    /** Tìm người có cùng tên (không phân biệt hoa thường, dấu cách thừa). */
    fun contactByName(name: String): ContactItem? {
        val n = name.trim().replace(Regex("\\s+"), " ").lowercase()
        if (n.isEmpty()) return null
        return visibleContacts.firstOrNull { it.name.trim().replace(Regex("\\s+"), " ").lowercase() == n }
    }

    fun contactFor(number: String): ContactItem? = byKey[PhoneUtil.key(number)]

    fun contactById(id: Long): ContactItem? = contacts.firstOrNull { it.id == id }

    /** Số ngày từ lần sao lưu gần nhất; null = chưa sao lưu lần nào. */
    fun daysSinceBackup(): Long? =
        if (lastBackup == 0L) null else (System.currentTimeMillis() - lastBackup) / 86_400_000L
}

/** Nhỏ, Vừa, Lớn, Rất lớn. */
val FONT_FACTORS = floatArrayOf(0.9f, 1.0f, 1.15f, 1.3f)
val FONT_NAMES = listOf("Nhỏ", "Vừa", "Lớn", "Rất lớn")
