package com.quoctuan.danhbaso.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quoctuan.danhbaso.data.AppData
import com.quoctuan.danhbaso.data.ContactItem
import com.quoctuan.danhbaso.util.Actions
import com.quoctuan.danhbaso.util.TextUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Màn Danh bạ: vòng tròn Gmail góc trên, Yêu thích, danh sách theo chữ cái. */
@Composable
fun ContactsScreen(
    data: AppData,
    focusSearch: Boolean,
    onOpen: (ContactItem) -> Unit,
    onAdd: () -> Unit,
    onChanged: () -> Unit,
    onGroupCall: (List<Long>, String?) -> Unit = { _, _ -> },
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var query by rememberSaveable { mutableStateOf("") }
    var showAccounts by remember { mutableStateOf(false) }
    var showAddFav by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(focusSearch) { if (focusSearch) runCatching { focus.requestFocus() } }

    var showCreateGroup by remember { mutableStateOf(false) }

    // Chọn nhiều: giữ tay vào một người để bắt đầu chọn
    var selected by remember { mutableStateOf(setOf<Long>()) }
    val selecting = selected.isNotEmpty()
    var bulkDelete by remember { mutableStateOf(false) }
    var bulkGroup by remember { mutableStateOf(false) }
    fun toggle(id: Long) { selected = if (id in selected) selected - id else selected + id }
    androidx.activity.compose.BackHandler(enabled = selecting) { selected = emptySet() }
    // Nhóm đang lọc (null = Tất cả)
    var groupSel by rememberSaveable { mutableStateOf<Long?>(null) }
    LaunchedEffect(data.groups) { if (groupSel != null && data.groups.none { it.id == groupSel }) groupSel = null }
    val base = data.visibleContacts
    val list = remember(base, groupSel, data.groupMembers) {
        val sel = groupSel
        if (sel == null) base else {
            val m = data.groupMembers[sel] ?: emptySet()
            base.filter { it.id in m }
        }
    }
    val favs = remember(list) { list.filter { it.starred && !it.simOnly } }
    val filtered = remember(list, query) {
        if (query.isBlank()) list else {
            val q = TextUtil.removeDiacritics(query).lowercase().trim()
            val qd = query.filter { ch -> ch.isDigit() }
            list.filter { it.searchKey.contains(q) || (qd.isNotEmpty() && it.digits.contains(qd)) }
        }
    }
    val groups = remember(filtered) { filtered.groupBy { it.letter } }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 96.dp)) {
            if (selecting) item {
                // Thanh "Đã chọn N" khi đang chọn nhiều
                Row(
                    Modifier.padding(16.dp, 12.dp, 16.dp, 8.dp).fillMaxWidth().heightIn(min = 56.dp)
                        .clip(RoundedCornerShape(28.dp)).background(C.BlueBg).glowBorder(RoundedCornerShape(28.dp)).padding(start = 4.dp, end = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.material3.IconButton(onClick = { selected = emptySet() }) {
                        Icon(Icons.Filled.Close, "Bỏ chọn", tint = C.Text)
                    }
                    Text("Đã chọn ${selected.size}", color = C.Text, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text(
                        if (selected.size == filtered.size) "Bỏ chọn hết" else "Chọn tất cả",
                        color = C.Blue, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable {
                            selected = if (selected.size == filtered.size) emptySet() else filtered.map { it.id }.toSet()
                        }.padding(8.dp)
                    )
                }
            } else item {
                Row(
                    Modifier.padding(16.dp, 12.dp, 16.dp, 8.dp).fillMaxWidth().heightIn(min = 56.dp)
                        .clip(RoundedCornerShape(28.dp)).background(C.Search).glowBorder(RoundedCornerShape(28.dp)).padding(start = 16.dp, end = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Search, null, tint = C.Sub2)
                    Spacer(Modifier.width(12.dp))
                    Box(Modifier.weight(1f)) {
                        if (query.isEmpty()) Text("Tìm kiếm trong danh bạ", color = C.Sub, fontSize = 16.sp)
                        BasicTextField(
                            value = query, onValueChange = { query = it }, singleLine = true,
                            textStyle = TextStyle(color = C.Text, fontSize = 16.sp),
                            cursorBrush = SolidColor(C.Blue),
                            modifier = Modifier.fillMaxWidth().focusRequester(focus)
                        )
                    }
                    // Vòng tròn tài khoản Gmail
                    val acc = data.selectedAccount
                    Box(
                        Modifier.size(44.dp).glow(CircleShape, 10.dp).clip(CircleShape).background(if (acc != null) avatarColor(acc) else C.Card2)
                            .border(2.dp, C.accent, CircleShape).clickable { showAccounts = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(acc?.take(1)?.uppercase() ?: "?", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                }
            }
            // Thanh nhóm: chỉ có khi máy có tài khoản Gmail
            if (data.selectedAccount != null) {
                item { GroupBar(data, groupSel, onSelect = { groupSel = it }, onCreate = { showCreateGroup = true }) }
                groupSel?.let { gid ->
                    item {
                        GroupManageRow(data, gid, onDeleted = { groupSel = null }, onCall = { title ->
                            onGroupCall((data.groupMembers[gid] ?: emptySet()).toList(), title)
                        })
                    }
                }
            }
            if (query.isBlank() && groupSel == null && !selecting) {
                item {
                    Row(Modifier.fillMaxWidth().padding(20.dp, 8.dp, 20.dp, 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Yêu thích", color = C.Blue, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        // Gọi nhóm: chọn nhiều người, app gọi lần lượt rồi gộp
                        Row(
                            Modifier.clip(RoundedCornerShape(14.dp)).background(C.Green.copy(alpha = 0.14f))
                                .clickable { onGroupCall(emptyList(), null) }.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Groups, null, tint = C.Green, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Gọi nhóm", color = C.Green, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(10.dp))
                        Text("${list.size} liên hệ", color = C.Sub, fontSize = 13.sp)
                    }
                }
                item {
                    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        items(favs, key = { it.id }) { f ->
                            Column(Modifier.width(66.dp).clickable { onOpen(f) }, horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(Modifier.glow(CircleShape, 10.dp).border(2.dp, C.accent, CircleShape).padding(3.dp)) {
                                    Avatar(f.name, 56.dp, letter = f.letter)
                                }
                                Gap(6)
                                Text(f.name, color = C.Text, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        item {
                            Column(Modifier.width(66.dp).clickable { showAddFav = true }, horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(Modifier.size(60.dp).clip(CircleShape).background(C.Search), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Filled.Add, "Thêm yêu thích", tint = C.Sub2)
                                }
                                Gap(6)
                                Text("Thêm", color = C.Sub, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
            if (filtered.isEmpty() && data.loaded) {
                item { Text(if (query.isNotBlank()) "Không tìm thấy" else if (groupSel != null) "Nhóm này chưa có ai. Mở trang một người → bấm \"＋ Nhóm\" để thêm." else "Danh bạ trống", color = C.Sub, modifier = Modifier.padding(24.dp)) }
            }
            // Danh sách theo chữ cái, mỗi nhóm là một khối bo tròn
            for ((letter, people) in groups) {
                item(key = "L$letter") {
                    Text(letter, color = C.Sub2, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 32.dp, top = 14.dp, bottom = 8.dp))
                }
                val n = people.size
                people.forEachIndexed { i, p ->
                    item(key = p.id) {
                        val big = 18.dp
                        val small = 4.dp
                        val shape = when {
                            n == 1 -> RoundedCornerShape(big)
                            i == 0 -> RoundedCornerShape(big, big, small, small)
                            i == n - 1 -> RoundedCornerShape(small, small, big, big)
                            else -> RoundedCornerShape(small)
                        }
                        Row(
                            Modifier.padding(start = 16.dp, end = 16.dp, bottom = 3.dp).fillMaxWidth().clip(shape)
                                .background(if (p.id in selected) C.BlueBg else C.Card2).glowBorder(shape)
                                .combinedClickable(
                                    onClick = { if (selecting) toggle(p.id) else onOpen(p) },
                                    onLongClick = { toggle(p.id) }
                                )
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (selecting) {
                                val on = p.id in selected
                                Box(
                                    Modifier.size(26.dp).clip(CircleShape)
                                        .then(if (on) Modifier.background(C.Green) else Modifier.border(2.dp, C.TickOff, CircleShape)),
                                    contentAlignment = Alignment.Center
                                ) { if (on) Icon(Icons.Filled.Check, null, tint = C.OnGreen, modifier = Modifier.size(18.dp)) }
                                Spacer(Modifier.width(12.dp))
                            }
                            Avatar(p.name, 42.dp, letter = p.letter)
                            Spacer(Modifier.width(16.dp))
                            Text(p.name, color = C.Text, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
        if (selecting) {
            // Thanh hành động khi đang chọn nhiều
            val chosen = list.filter { it.id in selected }
            Row(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(C.Nav)
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                if (data.selectedAccount != null) BulkAction("Thêm vào nhóm", Icons.Filled.Groups, C.Blue) { bulkGroup = true }
                BulkAction("Gửi danh thiếp", Icons.Filled.Share, C.Blue) { Actions.shareContacts(ctx, chosen) }
                BulkAction("Xóa", Icons.Filled.Delete, C.Red) { bulkDelete = true }
            }
        } else Box(
            Modifier.align(Alignment.BottomEnd).padding(20.dp).size(62.dp)
                .glow(RoundedCornerShape(20.dp), 16.dp).clip(RoundedCornerShape(20.dp)).background(C.accent).clickable(onClick = onAdd),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Filled.Add, "Thêm liên hệ mới", tint = C.OnBlue, modifier = Modifier.size(30.dp)) }
    }

    if (bulkGroup) BulkGroupDialog(data, list.filter { it.id in selected }, onDismiss = { bulkGroup = false }) {
        bulkGroup = false
        selected = emptySet()
    }
    if (bulkDelete) {
        val chosen = list.filter { it.id in selected }
        AlertDialog(
            onDismissRequest = { bulkDelete = false },
            containerColor = C.Nav,
            title = { Text("Xóa ${chosen.size} người?", color = C.Text) },
            text = {
                Text(
                    "Xóa khỏi máy, Gmail và SIM: " + chosen.take(5).joinToString(", ") { it.name } +
                        (if (chosen.size > 5) "… và ${chosen.size - 5} người khác" else "") + ". Không hoàn tác được.",
                    color = C.Sub2
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    bulkDelete = false
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            for (c in chosen) {
                                if (!c.simOnly) data.contactsRepo.deleteContact(c.id)
                                if (c.inSim) c.phones.forEach { data.simRepo.deleteNumber(it) }
                            }
                        }
                        Actions.toast(ctx, "Đã xóa ${chosen.size} người")
                        selected = emptySet()
                        onChanged()
                    }
                }) { Text("Xóa", color = C.Red, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { bulkDelete = false }) { Text("Hủy", color = C.Sub2) } }
        )
    }
    if (showCreateGroup) CreateGroupDialog(data, onDismiss = { showCreateGroup = false }) { groupSel = it }
    if (showAccounts) AccountSheet(data, onDismiss = { showAccounts = false }, onChanged = onChanged)
    if (showAddFav) AddFavoriteDialog(data, list.filter { !it.starred && !it.simOnly }, onDismiss = { showAddFav = false }, onChanged = onChanged)
}

/** Bảng chọn tài khoản Gmail đang dùng (mở từ vòng tròn góc trên). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSheet(data: AppData, onDismiss: () -> Unit, onChanged: () -> Unit) {
    val ctx = LocalContext.current
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var picked by remember { mutableStateOf(data.selectedAccount) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = state, containerColor = C.Nav) {
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Tài khoản Gmail đang dùng", color = C.Text, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Text("Số mới và số khôi phục sẽ lưu vào tài khoản này", color = C.Sub, fontSize = 13.sp)
            if (data.accounts.isEmpty()) {
                Text("Chưa có Gmail nào trên máy.", color = C.Orange, fontSize = 14.sp)
            }
            for (a in data.accounts) {
                val sel = a == picked
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(if (sel) C.GreenBg else C.Card)
                        .border(2.dp, if (sel) C.Green else C.Line, RoundedCornerShape(16.dp))
                        .clickable { picked = a }.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Avatar(a, 40.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(a, color = C.Text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${data.accountCounts[a] ?: 0} liên hệ" + if (a == data.selectedAccount) " · đang chọn" else "", color = C.Sub, fontSize = 12.sp)
                    }
                    Box(
                        Modifier.size(28.dp).clip(CircleShape).background(if (sel) C.Green else Color.Transparent)
                            .border(2.dp, if (sel) C.Green else C.TickOff, CircleShape),
                        contentAlignment = Alignment.Center
                    ) { if (sel) Icon(Icons.Filled.Check, null, tint = C.OnGreen, modifier = Modifier.size(16.dp)) }
                }
            }
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).border(1.dp, C.Border, RoundedCornerShape(16.dp))
                    .clickable { Actions.addGoogleAccount(ctx) }.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(40.dp).clip(CircleShape).background(C.Search), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Add, null, tint = C.Blue)
                }
                Spacer(Modifier.width(14.dp))
                Text("Thêm tài khoản Gmail khác", color = C.Blue, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
            Button(
                onClick = {
                    picked?.let { data.selectAccount(it) }
                    onChanged()
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp), shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = C.Blue, contentColor = C.OnBlue)
            ) { Text("Xong", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
        }
    }
}

@Composable
private fun AddFavoriteDialog(data: AppData, candidates: List<ContactItem>, onDismiss: () -> Unit, onChanged: () -> Unit) {
    val scope = rememberCoroutineScope()
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = C.Nav,
        title = { Text("Thêm vào Yêu thích", color = C.Text) },
        text = {
            LazyColumn(Modifier.heightIn(max = 420.dp)) {
                items(candidates, key = { it.id }) { c ->
                    Row(
                        Modifier.fillMaxWidth().clickable {
                            scope.launch {
                                withContext(Dispatchers.IO) { data.contactsRepo.setStarred(c.id, true) }
                                onChanged()
                                onDismiss()
                            }
                        }.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Avatar(c.name, 36.dp)
                        Spacer(Modifier.width(12.dp))
                        Text(c.name, color = C.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Đóng") } }
    )
}

/** Nút trên thanh hành động khi chọn nhiều. */
@Composable
private fun BulkAction(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, onClick: () -> Unit) {
    Column(
        Modifier.clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(26.dp))
        Gap(4)
        Text(text, color = color, fontSize = 13.sp)
    }
}
