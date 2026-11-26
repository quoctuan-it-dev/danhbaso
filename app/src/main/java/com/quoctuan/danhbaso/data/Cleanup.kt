package com.quoctuan.danhbaso.data

import com.quoctuan.danhbaso.util.PhoneUtil

/** Một nhóm liên hệ bị trùng (trùng tên hoặc trùng số). */
data class DupGroup(
    /** true = trùng tên, false = trùng số. */
    val byName: Boolean,
    /** Tên (nếu trùng tên) hoặc số (nếu trùng số) để hiển thị. */
    val label: String,
    val items: List<ContactItem>,
    /** Người sẽ được giữ lại sau khi gộp. */
    val keep: ContactItem,
)

/** Kết quả chép SIM ↔ Gmail. */
data class CopyResult(val added: Int, val skipped: Int, val shortened: Int, val failed: List<String>)

/**
 * Các công cụ dọn dẹp danh bạ: tìm & gộp người trùng, chép SIM ↔ Gmail.
 * Mọi hàm ghi đều chạy nền (gọi trong Dispatchers.IO).
 */
class Cleanup(private val contacts: ContactsRepo, private val sim: SimRepo) {

    private fun normName(s: String) = s.trim().replace(Regex("\\s+"), " ").lowercase()

    /** Tên chỉ toàn số (do lưu không có tên) thì coi như chưa có tên. */
    private fun hasRealName(c: ContactItem) = c.name.any { it.isLetter() }

    /** Chọn người giữ lại: có tên thật > tên có dấu > nằm trong Gmail đang chọn > nhiều số hơn > yêu thích. */
    private fun pickKeep(items: List<ContactItem>, account: String?): ContactItem =
        items.sortedWith(
            compareByDescending<ContactItem> { hasRealName(it) }
                // Tên có dấu (tên gốc đầy đủ) được chọn sẵn trước tên không dấu (thường là tên chép từ SIM)
                .thenByDescending { it.name.any { ch -> ch.code > 127 } }
                .thenByDescending { account != null && account in it.googleAccounts }
                .thenByDescending { it.phones.size }
                .thenByDescending { it.starred }
        ).first()

    /** Tìm các nhóm trùng. Người chỉ có trong SIM không tính (SIM không gộp được). */
    fun find(all: List<ContactItem>, account: String?): List<DupGroup> {
        val real = all.filter { !it.simOnly }
        val result = ArrayList<DupGroup>()
        val used = HashSet<Long>()

        // 1) Trùng tên (chỉ tên có chữ, không tính tên toàn số)
        real.filter { hasRealName(it) }.groupBy { normName(it.name) }.values
            .filter { it.size >= 2 }
            .forEach { g ->
                result += DupGroup(true, g.first().name, g, pickKeep(g, account))
                used += g.map { it.id }
            }

        // 2) Trùng số (giữa những người chưa nằm trong nhóm trùng tên)
        val byKey = HashMap<String, MutableList<ContactItem>>()
        for (c in real) {
            if (c.id in used) continue
            for (p in c.phones) {
                val k = PhoneUtil.key(p)
                if (k.length < 6) continue
                val l = byKey.getOrPut(k) { ArrayList() }
                if (l.none { it.id == c.id }) l += c
            }
        }
        val seen = HashSet<Set<Long>>()
        for ((_, g) in byKey) {
            if (g.size < 2) continue
            val ids = g.map { it.id }.toSet()
            if (!seen.add(ids) || ids.any { it in used }) continue
            used += ids
            val number = g.flatMap { it.phones }.firstOrNull { p -> g.all { c -> c.phones.any { PhoneUtil.key(it) == PhoneUtil.key(p) } } }
                ?: g.first().firstPhone
            result += DupGroup(false, PhoneUtil.pretty(number), g, pickKeep(g, account))
        }
        return result
    }

    /**
     * Gộp theo lựa chọn của người dùng: một [name] và các số [phones] được giữ.
     * Người giữ lại = người có đúng tên đã chọn (nếu có), không thì người app tự chọn.
     * Đổi tên nếu cần, thêm số còn thiếu, bỏ số không chọn, rồi xóa những người còn lại.
     */
    fun mergeCustom(group: DupGroup, name: String, phones: List<String>, account: String?): Boolean {
        if (phones.isEmpty()) return false
        val keep = group.items.firstOrNull { it.name == name } ?: group.keep
        val wanted = phones.associateBy { PhoneUtil.key(it) }
        var ok = true
        // 1) Tên
        if (keep.name != name) ok = contacts.updateNameAndNumber(keep.id, name, keep.firstPhone, keep.firstPhone) && ok
        // 2) Bỏ số không chọn của người giữ lại
        for (p in keep.phones) if (PhoneUtil.key(p) !in wanted) ok = contacts.removePhone(keep.id, p) && ok
        // 3) Thêm số đã chọn mà người giữ lại chưa có
        val have = keep.phones.map { PhoneUtil.key(it) }.toSet()
        for ((k, p) in wanted) if (k !in have) ok = contacts.addPhoneToContact(keep.id, p, account) && ok
        // 4) Giữ Yêu thích, rồi xóa người còn lại (chỉ khi các bước trên thành công)
        if (!ok) return false
        val others = group.items.filter { it.id != keep.id }
        if (others.any { it.starred } && !keep.starred) contacts.setStarred(keep.id, true)
        for (o in others) contacts.deleteContact(o.id)
        return true
    }

    /** Số trong SIM mà Gmail [account] chưa có. */
    fun simNotInGmail(account: String): List<SimRecord> {
        val have = contacts.numberKeysInAccount(account)
        val seen = HashSet<String>()
        return sim.records().filter {
            val k = PhoneUtil.key(it.number)
            k.isNotEmpty() && k !in have && seen.add(k)
        }
    }

    /** Người trong Gmail [account] mà SIM chưa có (so theo số đầu tiên). */
    fun gmailNotInSim(all: List<ContactItem>, account: String): List<ContactItem> {
        val simKeys = sim.records().map { PhoneUtil.key(it.number) }.toHashSet()
        return all.filter { !it.simOnly && account in it.googleAccounts && PhoneUtil.key(it.firstPhone) !in simKeys }
    }

    fun copySimToGmail(list: List<SimRecord>, account: String, onProgress: (Int, Int) -> Unit): CopyResult {
        var added = 0
        val failed = ArrayList<String>()
        list.forEachIndexed { i, r ->
            onProgress(i + 1, list.size)
            val name = r.name.ifBlank { PhoneUtil.pretty(r.number) }
            if (contacts.insertGoogle(account, name, listOf(r.number), false)) added++ else failed += name
        }
        return CopyResult(added, 0, 0, failed)
    }

    fun copyGmailToSim(list: List<ContactItem>, onProgress: (Int, Int) -> Unit): CopyResult {
        var added = 0
        var short = 0
        var full = false
        val failed = ArrayList<String>()
        list.forEachIndexed { i, c ->
            onProgress(i + 1, list.size)
            if (full) { failed += "${c.name} (SIM đầy)"; return@forEachIndexed }
            when (sim.insert(c.name, c.firstPhone)) {
                SimWriteResult.OK -> added++
                SimWriteResult.SHORTENED -> { added++; short++ }
                SimWriteResult.FULL -> { full = true; failed += "${c.name} (SIM đầy)" }
                SimWriteResult.NO_SIM -> failed += "${c.name} (không thấy SIM)"
                SimWriteResult.ERROR -> failed += "${c.name} (máy không cho ghi)"
            }
        }
        return CopyResult(added, 0, short, failed)
    }
}
