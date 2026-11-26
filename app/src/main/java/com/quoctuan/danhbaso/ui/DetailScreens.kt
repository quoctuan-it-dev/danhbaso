package com.quoctuan.danhbaso.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quoctuan.danhbaso.data.AppData
import com.quoctuan.danhbaso.data.CallEntry
import com.quoctuan.danhbaso.data.ContactItem
import com.quoctuan.danhbaso.util.Actions
import com.quoctuan.danhbaso.util.PhoneUtil
import com.quoctuan.danhbaso.util.TextUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Màn bấm vào tên: Gọi / Nhắn tin / Zalo / Yêu thích, Sửa / Xóa / Chặn số, lịch sử cuộc gọi. */
@Composable
fun ContactDetailScreen(
    data: AppData,
    contact: ContactItem,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onChanged: () -> Unit,
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var confirmDelete by remember { mutableStateOf(false) }
    var showGroups by remember { mutableStateOf(false) }
    val keys = contact.phones.map { PhoneUtil.key(it) }.toSet()
    val history = data.calls.filter { PhoneUtil.key(it.number) in keys }
    val number = contact.firstPhone

    val savedIn = buildList {
        if (contact.googleAccounts.isNotEmpty()) add("Gmail")
        if (contact.inSim) add("SIM")
    }.joinToString(" + ").ifEmpty { "Bộ nhớ máy" }

    val acc = data.selectedAccount
    HistoryList(
        history = history,
        onBack = onBack,
        actions = {
            if (!contact.simOnly && acc != null) {
                Row(
                    Modifier.clip(RoundedCornerShape(18.dp)).border(1.5.dp, C.Blue, RoundedCornerShape(18.dp))
                        .clickable {
                            if (acc in contact.googleAccounts) showGroups = true
                            else Actions.toast(ctx, "Người này chưa lưu trong Gmail. Bấm Sửa → chọn \"Lưu vào Gmail\" trước.")
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Groups, null, tint = C.Blue, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Nhóm", color = C.Blue, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        header = {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Avatar(contact.name, 96.dp)
                Gap(10)
                Text(contact.name, color = C.Text, fontSize = 24.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                for (p in contact.phones) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            PhoneUtil.pretty(p) + " · Di động", color = C.Sub2, fontSize = 17.sp,
                            modifier = Modifier.clickable { Actions.call(ctx, p) }.padding(2.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        CopyButton { Actions.copy(ctx, p) }
                    }
                }
                Box(Modifier.padding(top = 4.dp).clip(RoundedCornerShape(10.dp)).background(C.GreenBg).padding(horizontal = 10.dp, vertical = 3.dp)) {
                    Text("Đã lưu: $savedIn", color = C.Green, fontSize = 12.sp)
                }
                // Nhóm của người này (bấm để sửa); nút "＋ Nhóm" nằm ở góc trên bên phải
                val tags = if (contact.simOnly) emptyList() else data.groupsOf(contact.id)
                if (tags.isNotEmpty()) {
                    Gap(8)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (g in tags) {
                            Box(
                                Modifier.clip(RoundedCornerShape(12.dp)).background(C.BlueBg)
                                    .clickable { showGroups = true }.padding(horizontal = 10.dp, vertical = 5.dp)
                            ) { Text(g.title, color = C.Blue, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
                        }
                    }
                }
            }
            Gap(18)
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                RoundAction("Gọi", C.GreenDark, C.Green, { Actions.call(ctx, number) }, icon = Icons.Filled.Call)
                RoundAction("Nhắn tin", C.Search, C.Sub2, { Actions.sms(ctx, number) }, icon = Icons.Filled.Sms)
                RoundAction("Mở Zalo", C.Zalo, Color.White, { Actions.zalo(ctx, number) }, text = "Zalo")
                if (!contact.simOnly) {
                    RoundAction(
                        "Yêu thích", C.Search, C.Yellow,
                        {
                            scope.launch {
                                withContext(Dispatchers.IO) { data.contactsRepo.setStarred(contact.id, !contact.starred) }
                                onChanged()
                            }
                        },
                        icon = if (contact.starred) Icons.Filled.Star else Icons.Filled.StarBorder
                    )
                }
            }
            Gap(14)
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallOutlined("Sửa", Icons.Filled.Edit, C.Text, Modifier.weight(1f), onEdit)
                SmallOutlined("Xóa", Icons.Filled.Delete, C.Red, Modifier.weight(1f)) { confirmDelete = true }
                SmallOutlined("Chặn số", Icons.Filled.Block, C.Orange, Modifier.weight(1f)) { Actions.block(ctx, number) }
            }
            Gap(10)
            // Gửi danh thiếp: tên + các số qua Zalo, tin nhắn...
            Row(
                Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(50.dp).clip(RoundedCornerShape(14.dp))
                    .border(1.5.dp, C.Blue, RoundedCornerShape(14.dp))
                    .clickable { Actions.shareContact(ctx, contact.name, contact.phones) },
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Share, null, tint = C.Blue, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Gửi danh thiếp", color = C.Blue, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Gap(18)
        }
    )

    if (showGroups) PickGroupsDialog(data, contact) { showGroups = false }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = C.Nav,
            title = { Text("Xóa liên hệ?", color = C.Text) },
            text = { Text("Xóa \"${contact.name}\" khỏi máy, Gmail và SIM. Không hoàn tác được.", color = C.Sub2) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            if (!contact.simOnly) data.contactsRepo.deleteContact(contact.id)
                            if (contact.inSim) contact.phones.forEach { data.simRepo.deleteNumber(it) }
                        }
                        Actions.toast(ctx, "Đã xóa ${contact.name}")
                        onChanged()
                        onBack()
                    }
                }) { Text("Xóa", color = C.Red) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Hủy") } }
        )
    }
}

/** Màn bấm vào số lạ (chưa lưu): Lưu số này / Thêm vào người có sẵn. */
@Composable
fun NumberDetailScreen(
    data: AppData,
    number: String,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onAddToExisting: () -> Unit,
) {
    val ctx = LocalContext.current
    val key = PhoneUtil.key(number)
    val history = data.calls.filter { PhoneUtil.key(it.number) == key }
    HistoryList(
        history = history,
        onBack = onBack,
        header = {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Avatar(null, 96.dp)
                Gap(10)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(PhoneUtil.pretty(number), color = C.Text, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.width(8.dp))
                    CopyButton { Actions.copy(ctx, number) }
                }
                Text("Số chưa lưu", color = C.Sub, fontSize = 14.sp)
                Gap(16)
                Button(
                    onClick = onSave, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = C.Blue, contentColor = C.OnBlue)
                ) {
                    Icon(Icons.Filled.PersonAdd, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Lưu số này", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Gap(8)
                OutlinedButton(onClick = onAddToExisting, modifier = Modifier.fillMaxWidth().height(44.dp), shape = RoundedCornerShape(14.dp)) {
                    Text("Thêm vào người có sẵn", color = C.Text)
                }
            }
            Gap(18)
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                RoundAction("Gọi", C.GreenDark, C.Green, { Actions.call(ctx, number) }, icon = Icons.Filled.Call)
                RoundAction("Nhắn tin", C.Search, C.Sub2, { Actions.sms(ctx, number) }, icon = Icons.Filled.Sms)
                RoundAction("Mở Zalo", C.Zalo, Color.White, { Actions.zalo(ctx, number) }, text = "Zalo")
                RoundAction("Chặn số", C.Search, C.Orange, { Actions.block(ctx, number) }, icon = Icons.Filled.Block)
            }
            Gap(18)
        }
    )
}

/** Nút tròn nhỏ "Sao chép số" đặt cạnh số điện thoại. */
@Composable
private fun CopyButton(onClick: () -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(16.dp)).background(C.Search).clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.ContentCopy, null, tint = C.Blue, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text("Sao chép", color = C.Blue, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SmallOutlined(text: String, icon: ImageVector, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier.height(44.dp).clip(RoundedCornerShape(12.dp)).border(1.dp, C.Border, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, color = color, fontSize = 14.sp)
    }
}

/** Phần đầu (thông tin) + danh sách lịch sử cuộc gọi của riêng số đó. */
@Composable
private fun HistoryList(
    history: List<CallEntry>, onBack: () -> Unit,
    actions: @Composable () -> Unit = {},
    header: @Composable () -> Unit,
) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item { TopBar("", onBack, actions) }
        item { header() }
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
                Text("Lịch sử cuộc gọi", color = C.Blue, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text("${history.size} lần", color = C.Sub, fontSize = 13.sp)
            }
        }
        if (history.isEmpty()) item { Text("Chưa có cuộc gọi", color = C.Sub, modifier = Modifier.padding(20.dp)) }
        items(history) { e ->
            val st = statusOf(e)
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 3.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .background(C.Card).padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(st.color))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(st.label, color = st.color, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text((if (isOutgoing(e)) "" else "Gọi tới · ") + formatTime(e.date), color = C.Sub, fontSize = 12.sp)
                }
                Text(formatDuration(e.duration), color = C.Sub2, fontSize = 13.sp)
            }
        }
    }
}

/** Chọn người có sẵn để gắn thêm số lạ vào. */
@Composable
fun PickExistingScreen(data: AppData, number: String, onBack: () -> Unit, onDone: (Long) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var q by remember { mutableStateOf("") }
    val list = data.visibleContacts.filter { !it.simOnly && (q.isBlank() || TextUtil.matches(it.name, q)) }
    Column(Modifier.fillMaxSize()) {
        TopBar("Thêm ${PhoneUtil.pretty(number)} vào…", onBack)
        OutlinedTextField(
            value = q, onValueChange = { q = it }, singleLine = true,
            placeholder = { Text("Tìm tên") },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = C.Text, unfocusedTextColor = C.Text)
        )
        LazyColumn(Modifier.fillMaxSize().heightIn(min = 0.dp), contentPadding = PaddingValues(16.dp)) {
            items(list, key = { it.id }) { c ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable {
                        scope.launch {
                            val ok = withContext(Dispatchers.IO) { data.contactsRepo.addPhoneToContact(c.id, number, data.selectedAccount) }
                            Actions.toast(ctx, if (ok) "Đã thêm số vào ${c.name}" else "Không thêm được số")
                            if (ok) onDone(c.id)
                        }
                    }.padding(vertical = 10.dp, horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Avatar(c.name, 40.dp)
                    Spacer(Modifier.width(14.dp))
                    Text(c.name, color = C.Text, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}
