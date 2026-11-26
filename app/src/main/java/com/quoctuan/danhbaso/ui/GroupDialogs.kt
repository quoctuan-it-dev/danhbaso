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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quoctuan.danhbaso.data.AppData
import com.quoctuan.danhbaso.data.ContactItem
import com.quoctuan.danhbaso.util.Actions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Thanh nhóm ở màn Danh bạ: Tất cả · Gia đình 3 · Công việc 12 · + Nhóm.
 * [selected] = null là "Tất cả".
 */
@Composable
fun GroupBar(data: AppData, selected: Long?, onSelect: (Long?) -> Unit, onCreate: () -> Unit) {
    val visibleIds = remember(data.visibleContacts) { data.visibleContacts.map { it.id }.toSet() }
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { GroupChip("Tất cả", selected == null) { onSelect(null) } }
        items(data.groups, key = { it.id }) { g ->
            val n = (data.groupMembers[g.id] ?: emptySet()).count { it in visibleIds }
            GroupChip("${g.title} $n", selected == g.id) { onSelect(g.id) }
        }
        item { GroupChip("+ Nhóm", false, dashed = true, onClick = onCreate) }
    }
}

@Composable
private fun GroupChip(text: String, on: Boolean, dashed: Boolean = false, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        Modifier.clip(shape)
            .then(
                when {
                    on -> Modifier.glow(shape, 8.dp).background(C.accent)
                    dashed -> Modifier.border(1.dp, C.Blue, shape)
                    else -> Modifier.border(1.dp, C.Line, shape)
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text, fontSize = 14.sp,
            color = when { on -> C.OnBlue; dashed -> C.Blue; else -> C.Sub2 },
            fontWeight = if (on) FontWeight.Bold else FontWeight.Normal
        )
    }
}

/** Hộp tạo nhóm mới. Gọi [onCreated] với mã nhóm vừa tạo. */
@Composable
fun CreateGroupDialog(data: AppData, onDismiss: () -> Unit, onCreated: (Long) -> Unit = {}) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf("") }
    val acc = data.selectedAccount
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = C.Nav,
        title = { Text("Tạo nhóm mới", color = C.Text) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title, onValueChange = { title = it.take(40) }, singleLine = true,
                    label = { Text("Tên nhóm (ví dụ: Gia đình)") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Sentences
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = C.Text, unfocusedTextColor = C.Text,
                        focusedBorderColor = C.Blue, unfocusedBorderColor = C.Border,
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    if (acc == null) "Cần có tài khoản Gmail trên máy để tạo nhóm."
                    else "Nhóm lưu trong Gmail $acc, mở Google Danh bạ cũng thấy.",
                    color = C.Sub, fontSize = 13.sp
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = acc != null && title.isNotBlank(),
                onClick = {
                    val t = title.trim()
                    if (data.groups.any { it.title.equals(t, ignoreCase = true) }) {
                        Actions.toast(ctx, "Đã có nhóm \"$t\"")
                    } else scope.launch {
                        val id = withContext(Dispatchers.IO) { data.groupsRepo.create(acc!!, t) }
                        if (id == null) Actions.toast(ctx, "Không tạo được nhóm")
                        else {
                            data.reloadGroups()
                            onCreated(id)
                        }
                        onDismiss()
                    }
                }
            ) { Text("Tạo", color = C.Blue, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy", color = C.Sub2) } }
    )
}

/** Hộp chọn nhóm cho một người: tick để cho vào / bỏ ra, có nút tạo nhóm mới. */
@Composable
fun PickGroupsDialog(data: AppData, contact: ContactItem, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    val acc = data.selectedAccount
    var creating by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }

    fun toggle(groupId: Long, inGroup: Boolean) {
        if (acc == null || busy) return
        busy = true
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                if (inGroup) data.groupsRepo.removeMember(contact.id, groupId, acc)
                else data.groupsRepo.addMember(contact.id, groupId, acc)
            }
            if (!ok) Actions.toast(ctx, "Không lưu được, hãy thử lại")
            data.reloadGroups()
            busy = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = C.Nav,
        title = { Text("Nhóm của \"${contact.name}\"", color = C.Text) },
        text = {
            Column(
                Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (data.groups.isEmpty()) Text("Chưa có nhóm nào. Bấm \"Tạo nhóm mới\" bên dưới.", color = C.Sub, fontSize = 14.sp)
                for (g in data.groups) {
                    val inGroup = contact.id in (data.groupMembers[g.id] ?: emptySet())
                    val shape = RoundedCornerShape(14.dp)
                    Row(
                        Modifier.fillMaxWidth().clip(shape).background(C.Card)
                            .clickable { toggle(g.id, inGroup) }.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier.size(26.dp).clip(RoundedCornerShape(8.dp))
                                .then(if (inGroup) Modifier.background(C.Green) else Modifier.border(2.dp, C.TickOff, RoundedCornerShape(8.dp))),
                            contentAlignment = Alignment.Center
                        ) { if (inGroup) Icon(Icons.Filled.Check, null, tint = C.OnGreen, modifier = Modifier.size(18.dp)) }
                        Spacer(Modifier.width(14.dp))
                        Text(g.title, color = C.Text, fontSize = 17.sp)
                    }
                }
                val shape = RoundedCornerShape(14.dp)
                Row(
                    Modifier.fillMaxWidth().clip(shape).border(1.5.dp, C.Blue, shape)
                        .clickable { creating = true }.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Add, null, tint = C.Blue)
                    Spacer(Modifier.width(10.dp))
                    Text("Tạo nhóm mới…", color = C.Blue, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Text("Nhóm lưu theo tài khoản Gmail, mở Google Danh bạ cũng thấy.", color = C.Sub, fontSize = 12.sp)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Xong", color = C.Blue) } }
    )

    if (creating) {
        CreateGroupDialog(data, onDismiss = { creating = false }) { newId ->
            // Tạo xong thì cho luôn người này vào nhóm mới
            toggle(newId, inGroup = false)
        }
    }
}

/** Hàng dưới thanh nhóm khi đang chọn một nhóm: Đổi tên · Xóa nhóm. */
@Composable
fun GroupManageRow(data: AppData, groupId: Long, onDeleted: () -> Unit) {
    val g = data.groups.firstOrNull { it.id == groupId } ?: return
    var renaming by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Nhóm: ${g.title}", color = C.Text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), maxLines = 1)
        SmallAction("Đổi tên", Icons.Filled.Edit, C.Blue) { renaming = true }
        SmallAction("Xóa nhóm", Icons.Filled.Delete, C.Red) { deleting = true }
    }
    if (renaming) RenameGroupDialog(data, g) { renaming = false }
    if (deleting) DeleteGroupDialog(data, g, onDismiss = { deleting = false }, onDeleted = onDeleted)
}

@Composable
private fun SmallAction(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        Modifier.clip(shape).border(1.dp, C.Border, shape).clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text(text, color = color, fontSize = 13.sp)
    }
}

/** Hộp đổi tên nhóm. */
@Composable
fun RenameGroupDialog(data: AppData, group: com.quoctuan.danhbaso.data.GroupInfo, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf(group.title) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = C.Nav,
        title = { Text("Đổi tên nhóm", color = C.Text) },
        text = {
            OutlinedTextField(
                value = title, onValueChange = { title = it.take(40) }, singleLine = true,
                label = { Text("Tên nhóm") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = C.Text, unfocusedTextColor = C.Text,
                    focusedBorderColor = C.Blue, unfocusedBorderColor = C.Border,
                ),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                enabled = title.isNotBlank() && title.trim() != group.title,
                onClick = {
                    val t = title.trim()
                    if (data.groups.any { it.id != group.id && it.title.equals(t, ignoreCase = true) }) {
                        Actions.toast(ctx, "Đã có nhóm \"$t\"")
                    } else scope.launch {
                        val ok = withContext(Dispatchers.IO) { data.groupsRepo.rename(group.id, t) }
                        Actions.toast(ctx, if (ok) "Đã đổi tên thành \"$t\"" else "Không đổi được tên")
                        data.reloadGroups()
                        onDismiss()
                    }
                }
            ) { Text("Lưu", color = C.Blue, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy", color = C.Sub2) } }
    )
}

/** Hộp hỏi lại trước khi xóa nhóm. */
@Composable
fun DeleteGroupDialog(data: AppData, group: com.quoctuan.danhbaso.data.GroupInfo, onDismiss: () -> Unit, onDeleted: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val n = data.groupMembers[group.id]?.size ?: 0
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = C.Nav,
        title = { Text("Xóa nhóm \"${group.title}\"?", color = C.Text) },
        text = {
            Text(
                "Nhóm có $n người. Họ vẫn còn trong danh bạ, chỉ bỏ nhãn nhóm này.",
                color = C.Sub2, fontSize = 15.sp
            )
        },
        confirmButton = {
            TextButton(onClick = {
                scope.launch {
                    val ok = withContext(Dispatchers.IO) { data.groupsRepo.delete(group.id) }
                    Actions.toast(ctx, if (ok) "Đã xóa nhóm \"${group.title}\"" else "Không xóa được nhóm")
                    data.reloadGroups()
                    onDismiss()
                    if (ok) onDeleted()
                }
            }) { Text("Xóa", color = C.Red, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy", color = C.Sub2) } }
    )
}

/** Thêm nhiều người vào một nhóm (dùng khi chọn nhiều ở màn Danh bạ). */
@Composable
fun BulkGroupDialog(data: AppData, people: List<ContactItem>, onDismiss: () -> Unit, onDone: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val acc = data.selectedAccount
    var creating by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }

    fun addAll(groupId: Long, title: String) {
        if (acc == null || busy) return
        busy = true
        scope.launch {
            val members = data.groupMembers[groupId] ?: emptySet()
            var added = 0
            var skipped = 0
            withContext(Dispatchers.IO) {
                for (c in people) {
                    when {
                        c.id in members -> {}
                        c.simOnly || acc !in c.googleAccounts -> skipped++
                        data.groupsRepo.addMember(c.id, groupId, acc) -> added++
                        else -> skipped++
                    }
                }
            }
            data.reloadGroups()
            busy = false
            Actions.toast(ctx, "Đã thêm $added người vào \"$title\"" + if (skipped > 0) " (bỏ qua $skipped người chưa lưu trong Gmail)" else "")
            onDone()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = C.Nav,
        title = { Text("Thêm ${people.size} người vào nhóm", color = C.Text) },
        text = {
            Column(
                Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (g in data.groups) {
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(C.Card)
                            .clickable { addAll(g.id, g.title) }.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(g.title, color = C.Text, fontSize = 17.sp, modifier = Modifier.weight(1f))
                        Text("${data.groupMembers[g.id]?.size ?: 0}", color = C.Sub, fontSize = 14.sp)
                    }
                }
                val shape = RoundedCornerShape(14.dp)
                Row(
                    Modifier.fillMaxWidth().clip(shape).border(1.5.dp, C.Blue, shape)
                        .clickable { creating = true }.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Add, null, tint = C.Blue)
                    Spacer(Modifier.width(10.dp))
                    Text("Tạo nhóm mới…", color = C.Blue, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Hủy", color = C.Sub2) } }
    )

    if (creating) {
        CreateGroupDialog(data, onDismiss = { creating = false }) { newId ->
            addAll(newId, data.groups.firstOrNull { it.id == newId }?.title ?: "nhóm mới")
        }
    }
}
