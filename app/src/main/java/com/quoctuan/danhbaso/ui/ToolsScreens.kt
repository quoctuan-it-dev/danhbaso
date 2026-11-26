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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quoctuan.danhbaso.data.AppData
import com.quoctuan.danhbaso.data.ContactItem
import com.quoctuan.danhbaso.data.CopyResult
import com.quoctuan.danhbaso.data.DupGroup
import com.quoctuan.danhbaso.data.SimRecord
import com.quoctuan.danhbaso.util.Actions
import com.quoctuan.danhbaso.util.PhoneUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Nút chính (dải màu) dùng chung cho các màn công cụ. */
@Composable
internal fun PrimaryButton(text: String, enabled: Boolean = true, modifier: Modifier = Modifier, onClick: () -> Unit) {
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
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val acc = data.selectedAccount
    var skipped by remember { mutableStateOf(setOf<String>()) }
    var busy by remember { mutableStateOf(false) }
    var confirmAll by remember { mutableStateOf(false) }
    var confirmOne by remember { mutableStateOf<DupGroup?>(null) }
    val groups = remember(data.contacts, acc) { data.cleanup.find(data.contacts, acc) }
    val shown = groups.filter { groupId(it) !in skipped }

    fun doMerge(list: List<DupGroup>) {
        busy = true
        scope.launch {
            val ok = withContext(Dispatchers.IO) { list.count { data.cleanup.merge(it, acc) } }
            busy = false
            Actions.toast(ctx, if (ok == list.size) "Đã gộp $ok nhóm" else "Đã gộp $ok / ${list.size} nhóm, còn lại chưa gộp được")
            onChanged()
        }
    }

    Column(Modifier.fillMaxSize()) {
        TopBar("Dọn danh bạ trùng", onBack)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!data.loaded) {
                Text("Đang đọc danh bạ…", color = C.Sub2)
            } else if (shown.isEmpty()) {
                CardBox(bg = C.GreenBg) {
                    Text("✔ Danh bạ không còn ai trùng", color = C.Green, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Gap(4)
                    Text("Người chỉ lưu trong SIM không được tính.", color = C.Sub2, fontSize = 13.sp)
                }
            } else {
                Text(
                    "Tìm thấy ${shown.size} nhóm trùng. Gộp = thành 1 người, giữ đủ các số. Nên sao lưu trước khi dọn.",
                    color = C.Sub2, fontSize = 14.sp
                )
                for (g in shown) {
                    CardBox(modifier = Modifier.glowBorder(RoundedCornerShape(20.dp))) {
                        Text(
                            (if (g.byName) "Trùng tên: " else "Trùng số: ") + "${g.label} (${g.items.size})",
                            color = C.Text, fontWeight = FontWeight.Bold, fontSize = 16.sp
                        )
                        Gap(6)
                        for (c in g.items) {
                            Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Avatar(c.name, 34.dp, letter = c.letter)
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        c.name + if (c.id == g.keep.id) "  · giữ lại" else "",
                                        color = if (c.id == g.keep.id) C.Green else C.Text, fontSize = 15.sp
                                    )
                                    Text(
                                        c.phones.joinToString(", ") { PhoneUtil.pretty(it) } + " · " + savedWhere(c),
                                        color = C.Sub, fontSize = 13.sp
                                    )
                                }
                            }
                        }
                        Gap(8)
                        // Tên giữ lại nằm riêng một dòng cho dễ đọc; 2 nút cao bằng nhau
                        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PrimaryButton(
                                if (g.byName) "Gộp thành 1" else "Gộp, giữ tên\n\"${g.keep.name}\"",
                                enabled = !busy, modifier = Modifier.weight(1.4f).fillMaxHeight()
                            ) { confirmOne = g }
                            SecondaryButton("Bỏ qua", Modifier.weight(1f).fillMaxHeight()) { skipped = skipped + groupId(g) }
                        }
                    }
                }
                if (shown.size > 1) {
                    PrimaryButton("Gộp tất cả (${shown.size})", enabled = !busy, modifier = Modifier.fillMaxWidth()) { confirmAll = true }
                }
            }
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            Gap(24)
        }
    }

    confirmOne?.let { g ->
        ConfirmMergeDialog(
            text = "Gộp ${g.items.size} mục thành \"${g.keep.name}\". Các số được chuyển sang người giữ lại, những mục còn lại sẽ bị xóa.",
            onDismiss = { confirmOne = null },
            onOk = { confirmOne = null; doMerge(listOf(g)) }
        )
    }
    if (confirmAll) {
        ConfirmMergeDialog(
            text = "Gộp cả ${shown.size} nhóm? Mỗi nhóm còn lại 1 người, giữ đủ các số. Không hoàn tác được – nên sao lưu trước.",
            onDismiss = { confirmAll = false },
            onOk = { confirmAll = false; doMerge(shown) }
        )
    }
}

private fun groupId(g: DupGroup) = g.items.map { it.id }.sorted().joinToString(",")

@Composable
private fun ConfirmMergeDialog(text: String, onDismiss: () -> Unit, onOk: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = C.Nav,
        title = { Text("Gộp liên hệ?", color = C.Text) },
        text = { Text(text, color = C.Sub2, fontSize = 15.sp) },
        confirmButton = { TextButton(onClick = onOk) { Text("Gộp", color = C.Blue, fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy", color = C.Sub2) } }
    )
}

// ======================= CHÉP SIM ↔ GMAIL =======================

/** Màn "Chép SIM ↔ Gmail". */
@Composable
fun CopySimGmailScreen(data: AppData, onBack: () -> Unit, onChanged: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val acc = data.selectedAccount
    val sim = data.simInfo
    var simToGmail by remember { mutableStateOf<List<SimRecord>?>(null) }
    var gmailToSim by remember { mutableStateOf<List<ContactItem>?>(null) }
    var busy by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
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
