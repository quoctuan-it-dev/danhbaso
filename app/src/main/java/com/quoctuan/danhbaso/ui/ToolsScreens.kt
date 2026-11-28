package com.quoctuan.danhbaso.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material3.Icon
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quoctuan.danhbaso.data.AppData
import com.quoctuan.danhbaso.data.ContactItem
import com.quoctuan.danhbaso.data.CopyResult
import com.quoctuan.danhbaso.data.DupGroup
import com.quoctuan.danhbaso.data.SimRecord
import com.quoctuan.danhbaso.util.PhoneUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Nút chính (dải màu) dùng chung cho các màn công cụ. */
@Composable
internal fun PrimaryButton(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier.heightIn(min = 46.dp).clip(shape)
            .background(if (enabled) C.accent else androidx.compose.ui.graphics.SolidColor(C.Card2))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text, color = if (enabled) C.OnBlue else C.Sub, fontWeight = FontWeight.Bold, fontSize = 15.sp,
            textAlign = TextAlign.Center, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
}

@Composable
internal fun SecondaryButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier.heightIn(min = 46.dp).clip(shape).border(1.dp, C.Border, shape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Text(text, color = C.Sub2, fontWeight = FontWeight.SemiBold, fontSize = 15.sp) }
}

private fun savedWhere(c: ContactItem): String = buildList {
    if (c.googleAccounts.isNotEmpty()) add("Gmail")
    if (c.inSim) add("SIM")
    if (isEmpty()) add("Máy")
}.joinToString(" + ")

// ======================= DỌN DANH BẠ TRÙNG =======================

/** Màn "Dọn danh bạ trùng": tìm người trùng tên / trùng số và gộp lại. */
@Composable
fun DuplicatesScreen(data: AppData, onBack: () -> Unit, onChanged: () -> Unit) {
    val scope = rememberCoroutineScope()
    val acc = data.selectedAccount
    // Nhóm đã bấm "Không trùng, bỏ qua" – lưu lại, mở lại / cập nhật app không hiện nữa
    var ignored by remember { mutableStateOf(data.prefs.ignoredDupes) }
    var showHidden by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<MergeSummary?>(null) }
    var simRecs by remember { mutableStateOf<List<SimRecord>>(emptyList()) }
    LaunchedEffect(data.contacts) { simRecs = withContext(Dispatchers.IO) { data.cleanup.simRecords() } }
    var busy by remember { mutableStateOf(false) }
    var confirmAll by remember { mutableStateOf(false) }
    // Lựa chọn của người dùng cho từng nhóm (khóa = groupId)
    var nameSel by remember { mutableStateOf(mapOf<String, String>()) }     // tên đã chọn; "" = gõ tên khác
    var customName by remember { mutableStateOf(mapOf<String, String>()) }
    var phoneOff by remember { mutableStateOf(mapOf<String, Set<String>>()) } // các số (key) bị bỏ tick
    var confirmOne by remember { mutableStateOf<DupGroup?>(null) }
    val groups = remember(data.contacts, acc) { data.cleanup.find(data.contacts, acc) }
    val shown = groups.filter { groupId(it) !in ignored }
    val hidden = groups.filter { groupId(it) in ignored }
    val display = if (showHidden) groups else shown
    fun setIgnored(v: Set<String>) {
        // Chỉ giữ các nhóm còn tồn tại để danh sách không phình to
        val alive = groups.map { groupId(it) }.toSet()
        ignored = v.filter { it in alive }.toSet()
        data.prefs.ignoredDupes = ignored
    }

    fun chosenName(g: DupGroup): String {
        val id = groupId(g)
        val n = nameSel[id] ?: g.keep.name
        return if (n == "") (customName[id] ?: "").trim() else n
    }
    fun chosenPhones(g: DupGroup): List<String> {
        val off = phoneOff[groupId(g)] ?: emptySet()
        return allPhones(g).filter { PhoneUtil.key(it.first) !in off }.map { it.first }
    }

    fun doMerge(list: List<DupGroup>) {
        busy = true
        scope.launch {
            val res = withContext(Dispatchers.IO) {
                list.map { data.cleanup.mergeCustom(it, chosenName(it), chosenPhones(it), acc) }
            }
            busy = false
            result = MergeSummary(
                merged = res.count { it.ok }, total = list.size,
                simCleaned = res.sumOf { it.simCleaned }, failed = res.flatMap { it.failed }
            )
            onChanged()
        }
    }

    Column(Modifier.fillMaxSize()) {
        TopBar("Dọn danh bạ trùng", onBack)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            result?.let { r -> MergeResultCards(r) { result = null } }
            if (!data.loaded) {
                Text("Đang đọc danh bạ…", color = C.Sub2)
            } else if (display.isEmpty()) {
                CardBox(bg = C.GreenBg) {
                    Text("✔ Danh bạ không còn ai trùng", color = C.Green, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Gap(4)
                    Text("Người chỉ lưu trong SIM không được tính.", color = C.Sub2, fontSize = 13.sp)
                }
            } else {
                Text(
                    "Tìm thấy ${display.size} nhóm trùng. Gộp = thành 1 người, giữ đủ các số. Nên sao lưu trước khi dọn.",
                    color = C.Sub2, fontSize = 14.sp
                )
                for (g in display) {
                    CardBox(modifier = Modifier.glowBorder(RoundedCornerShape(20.dp))) {
                        Text(
                            (if (g.byName) "Trùng tên: " else "Trùng số: ") + "${g.label} (${g.items.size})",
                            color = C.Text, fontWeight = FontWeight.Bold, fontSize = 16.sp
                        )
                        val id = groupId(g)
                        // 1. Chọn tên giữ lại
                        Gap(8)
                        Text("1. Chọn tên giữ lại", color = C.Blue, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        val names = g.items.map { it.name }.distinct()
                        val curName = nameSel[id] ?: g.keep.name
                        for (n in names) ChoiceRow(radio = true, on = curName == n, title = n) { nameSel = nameSel + (id to n) }
                        ChoiceRow(radio = true, on = curName == "", title = "Gõ tên khác…") { nameSel = nameSel + (id to "") }
                        if (curName == "") {
                            androidx.compose.material3.OutlinedTextField(
                                value = customName[id] ?: "", onValueChange = { customName = customName + (id to it.take(60)) },
                                singleLine = true, label = { Text("Tên mới") },
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                    capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Words
                                ),
                                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = C.Text, unfocusedTextColor = C.Text,
                                    focusedBorderColor = C.Blue, unfocusedBorderColor = C.Border,
                                ),
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                            )
                        }
                        // 2. Chọn số giữ lại
                        Gap(10)
                        Text("2. Chọn số giữ lại", color = C.Blue, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        val off = phoneOff[id] ?: emptySet()
                        for ((p, owners) in allPhones(g)) {
                            val k = PhoneUtil.key(p)
                            ChoiceRow(radio = false, on = k !in off, title = PhoneUtil.pretty(p), sub = owners) {
                                phoneOff = phoneOff + (id to (if (k in off) off - k else off + k))
                            }
                        }
                        val nm = chosenName(g)
                        val nPhones = chosenPhones(g).size
                        // Bản trong SIM sẽ được dọn khi gộp
                        val simDirty = if (nm.isEmpty()) emptyList() else
                            chosenPhones(g).flatMap { data.cleanup.simNeedsCleanup(simRecs, PhoneUtil.key(it), nm) }
                        if (simDirty.isNotEmpty()) {
                            Gap(8)
                            val simNm = data.cleanup.simName(nm)
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(C.Search).padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.SimCard, null, tint = C.Blue, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    if (simDirty.size > 1) "Trong SIM đang có ${simDirty.size} bản số này. Khi gộp sẽ chỉ giữ 1 bản tên \"$simNm\"."
                                    else "Trong SIM đang lưu tên \"${simDirty[0].name}\". Khi gộp sẽ đổi thành \"$simNm\".",
                                    color = C.Text, fontSize = 13.sp
                                )
                            }
                        }
                        Gap(10)
                        PrimaryButton(
                            if (nm.isEmpty()) "Hãy nhập tên" else if (nPhones == 0) "Hãy chọn ít nhất 1 số"
                            else "Gộp thành\n\"$nm\" · $nPhones số",
                            enabled = !busy && nm.isNotEmpty() && nPhones > 0, modifier = Modifier.fillMaxWidth()
                        ) { confirmOne = g }
                        Gap(8)
                        if (id in ignored) {
                            SecondaryButton("Hiện lại nhóm này", Modifier.fillMaxWidth()) { setIgnored(ignored - id) }
                        } else {
                            SecondaryButton("Không trùng, bỏ qua", Modifier.fillMaxWidth()) { setIgnored(ignored + id) }
                        }
                    }
                }
                if (shown.size > 1) {
                    PrimaryButton("Gộp tất cả (${shown.size})", enabled = !busy, modifier = Modifier.fillMaxWidth()) { confirmAll = true }
                }
            }
            if (data.loaded && hidden.isNotEmpty()) {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(C.Card)
                        .clickable { showHidden = !showHidden }.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (showHidden) "Đang hiện cả ${hidden.size} nhóm đã ẩn" else "Đã ẩn ${hidden.size} nhóm không trùng",
                        color = C.Text, fontSize = 14.sp, modifier = Modifier.weight(1f)
                    )
                    Text(if (showHidden) "Ẩn đi ›" else "Xem lại ›", color = C.Blue, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            Gap(24)
        }
    }

    confirmOne?.let { g ->
        val nm = chosenName(g)
        val ph = chosenPhones(g)
        val dropped = allPhones(g).map { it.first }.filter { p -> ph.none { PhoneUtil.key(it) == PhoneUtil.key(p) } }
        ConfirmMergeDialog(
            title = "Gộp liên hệ?",
            text = "Còn lại 1 người: \"$nm\" với " + ph.joinToString(", ") { PhoneUtil.pretty(it) } + "." +
                (if (dropped.isNotEmpty()) " Bỏ số: " + dropped.joinToString(", ") { PhoneUtil.pretty(it) } + "." else "") +
                " Không hoàn tác được.",
            okText = "Gộp",
            onDismiss = { confirmOne = null },
            onOk = { confirmOne = null; doMerge(listOf(g)) }
        )
    }
    if (confirmAll) {
        ConfirmMergeDialog(
            title = "Gộp tất cả?",
            text = "Gộp cả ${shown.size} nhóm theo tên và số đang chọn ở từng nhóm? Không hoàn tác được – nên sao lưu trước.",
            okText = "Gộp tất cả",
            onDismiss = { confirmAll = false },
            onOk = { confirmAll = false; doMerge(shown) }
        )
    }
}

/** Mã nhóm bền: theo tên + số (không theo mã nội bộ của máy, vì mã này đổi sau khi đồng bộ). */
private fun groupId(g: DupGroup) = g.items.map { c ->
    c.name.trim().lowercase() + ":" + c.phones.map { PhoneUtil.key(it) }.distinct().sorted().joinToString("/")
}.sorted().joinToString("|")

/** Tổng kết sau khi gộp, hiện bằng thẻ trong trang (không dùng khung đen). */
private data class MergeSummary(val merged: Int, val total: Int, val simCleaned: Int, val failed: List<String>)

@Composable
private fun MergeResultCards(r: MergeSummary, onClose: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (r.merged > 0) ResultCard(
            ok = true, title = "Đã gộp ${r.merged} nhóm",
            sub = if (r.simCleaned > 0) "Đã dọn ${r.simCleaned} bản thừa trong SIM" else null, onClose = onClose
        )
        if (r.failed.isNotEmpty() || r.merged < r.total) ResultCard(
            ok = false,
            title = if (r.merged < r.total) "${r.total - r.merged} nhóm chưa gộp được" else "${r.failed.size} bản chưa xóa được",
            sub = (r.failed.joinToString("; ").ifEmpty { "Thử lại sau" }) + " – thử lại hoặc xóa bằng app Danh bạ của máy",
            onClose = if (r.merged > 0) null else onClose
        )
    }
}

@Composable
private fun ResultCard(ok: Boolean, title: String, sub: String?, onClose: (() -> Unit)?) {
    val col = if (ok) C.Green else C.Orange
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(C.Card)
            .border(1.5.dp, col.copy(alpha = 0.6f), RoundedCornerShape(16.dp)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(38.dp).clip(RoundedCornerShape(12.dp))
                .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(col.copy(alpha = 0.12f), col.copy(alpha = 0.24f)))),
            contentAlignment = Alignment.Center
        ) {
            if (ok) Icon(Icons.Filled.Check, null, tint = col, modifier = Modifier.size(22.dp))
            else Text("!", color = col, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = C.Text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            if (sub != null) Text(sub, color = C.Sub2, fontSize = 12.5.sp)
        }
        if (onClose != null) {
            Icon(
                Icons.Filled.Close, "Đóng", tint = C.Sub,
                modifier = Modifier.size(32.dp).clip(RoundedCornerShape(16.dp)).clickable(onClick = onClose).padding(6.dp)
            )
        }
    }
}

/** Mọi số trong nhóm (không lặp), kèm "của ai · lưu ở đâu". */
private fun allPhones(g: DupGroup): List<Pair<String, String>> {
    val seen = LinkedHashMap<String, Pair<String, MutableList<ContactItem>>>()
    for (c in g.items) for (p in c.phones) {
        val k = PhoneUtil.key(p)
        seen.getOrPut(k) { p to ArrayList() }.second += c
    }
    return seen.values.map { (p, owners) ->
        p to owners.distinctBy { it.id }.joinToString(", ") { it.name + " · " + savedWhere(it) }
    }
}

/** Một dòng chọn: nút tròn (chọn 1) hoặc ô vuông (chọn nhiều). */
@Composable
private fun ChoiceRow(radio: Boolean, on: Boolean, title: String, sub: String? = null, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(if (on) C.GreenBg else androidx.compose.ui.graphics.Color.Transparent)
            .clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val shape = if (radio) androidx.compose.foundation.shape.CircleShape else RoundedCornerShape(6.dp)
        Box(
            Modifier.size(22.dp).clip(shape)
                .then(if (!radio && on) Modifier.background(C.Green) else Modifier.border(2.dp, if (on) C.Green else C.TickOff, shape)),
            contentAlignment = Alignment.Center
        ) {
            if (on) {
                if (radio) Box(Modifier.size(11.dp).clip(shape).background(C.Green))
                else androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Filled.Check, null, tint = C.OnGreen, modifier = Modifier.size(16.dp))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = if (on) C.Text else C.Sub2, fontSize = 16.sp)
            if (sub != null) Text(sub, color = C.Sub, fontSize = 12.sp)
        }
    }
}

@Composable
private fun ConfirmMergeDialog(title: String, text: String, okText: String, onDismiss: () -> Unit, onOk: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = C.Nav,
        title = { Text(title, color = C.Text) },
        text = { Text(text, color = C.Sub2, fontSize = 15.sp) },
        confirmButton = {
            TextButton(onClick = onOk) {
                Text(okText, color = if (okText.startsWith("Xóa")) C.Red else C.Blue, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy", color = C.Sub2) } }
    )
}

// ======================= CHÉP SIM ↔ GMAIL =======================

/** Màn "Chép SIM ↔ Gmail". */
@Composable
fun CopySimGmailScreen(data: AppData, onBack: () -> Unit, onChanged: () -> Unit) {
    val scope = rememberCoroutineScope()
    val acc = data.selectedAccount
    val sim = data.simInfo
    var simToGmail by remember { mutableStateOf<List<SimRecord>?>(null) }
    var gmailToSim by remember { mutableStateOf<List<ContactItem>?>(null) }
    var busy by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var result by remember { mutableStateOf<Pair<String, CopyResult>?>(null) }

    // Đếm lại mỗi khi danh bạ thay đổi
    LaunchedEffect(data.contacts, acc) {
        if (acc != null && sim.available) {
            val (a, b) = withContext(Dispatchers.IO) {
                data.cleanup.simNotInGmail(acc) to data.cleanup.gmailNotInSim(data.contacts, acc)
            }
            simToGmail = a
            gmailToSim = b
        }
    }

    fun startCopy(label: String, block: suspend () -> CopyResult) {
        busy = true; progress = 0f; result = null
        scope.launch {
            val r = block()
            busy = false
            result = label to r
            onChanged()
        }
    }

    Column(Modifier.fillMaxSize()) {
        TopBar("Chép SIM ↔ Gmail", onBack)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when {
                acc == null -> CardBox { Text("Máy chưa có tài khoản Gmail. Thêm Gmail trong Cài đặt của điện thoại trước.", color = C.Orange) }
                !sim.available -> CardBox { Text("Không đọc được SIM (máy không có SIM hoặc không cho phép đọc).", color = C.Orange) }
                else -> {
                    Text(
                        "Gmail: $acc · SIM: " + (if (sim.maxRecords > 0) "${sim.used}/${sim.maxRecords} số" else "đang dùng"),
                        color = C.Sub2, fontSize = 14.sp
                    )
                    // SIM → Gmail
                    CardBox(modifier = Modifier.glowBorder(RoundedCornerShape(20.dp))) {
                        Text("📲  SIM → Gmail", color = C.Text, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Gap(6)
                        val n = simToGmail?.size
                        Text(
                            when (n) {
                                null -> "Đang kiểm tra…"
                                0 -> "Mọi số trong SIM đã có trong Gmail."
                                else -> "Có $n số trong SIM chưa có trong Gmail. Chép vào Gmail để không mất khi hư hoặc mất SIM."
                            },
                            color = C.Sub2, fontSize = 14.sp
                        )
                        Gap(10)
                        PrimaryButton("Chép ${n ?: 0} số vào Gmail", enabled = !busy && (n ?: 0) > 0, modifier = Modifier.fillMaxWidth()) {
                            val list = simToGmail ?: return@PrimaryButton
                            startCopy("Gmail") { withContext(Dispatchers.IO) { data.cleanup.copySimToGmail(list, acc) { i, t -> progress = i.toFloat() / t } } }
                        }
                    }
                    // Gmail → SIM
                    CardBox(modifier = Modifier.glowBorder(RoundedCornerShape(20.dp))) {
                        Text("💳  Gmail → SIM", color = C.Text, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Gap(6)
                        val n = gmailToSim?.size
                        Text(
                            when (n) {
                                null -> "Đang kiểm tra…"
                                0 -> "Mọi người trong Gmail đã có trong SIM."
                                else -> "Có $n số chưa có trong SIM" +
                                    (if (sim.free >= 0) ", SIM còn trống ${sim.free} chỗ" else "") +
                                    ". Tên dài sẽ được rút gọn, bỏ dấu. Mỗi người chỉ chép số đầu tiên."
                            },
                            color = C.Sub2, fontSize = 14.sp
                        )
                        Gap(10)
                        PrimaryButton("Chép ${n ?: 0} số vào SIM", enabled = !busy && (n ?: 0) > 0, modifier = Modifier.fillMaxWidth()) {
                            val list = gmailToSim ?: return@PrimaryButton
                            startCopy("SIM") { withContext(Dispatchers.IO) { data.cleanup.copyGmailToSim(list) { i, t -> progress = i.toFloat() / t } } }
                        }
                    }
                    if (busy) LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                    result?.let { (where, r) ->
                        CardBox(bg = C.GreenBg) {
                            Text("✔ Đã chép ${r.added} số vào $where", color = C.Green, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            if (r.shortened > 0) Text("${r.shortened} tên bị rút gọn cho vừa SIM", color = C.Sub2, fontSize = 13.sp)
                            if (r.failed.isNotEmpty()) {
                                Gap(4)
                                Text("Chưa chép được ${r.failed.size} số:", color = C.Orange, fontSize = 13.sp)
                                Text(r.failed.take(10).joinToString("\n"), color = C.Sub2, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
            Gap(24)
        }
    }
}
