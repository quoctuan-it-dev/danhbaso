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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quoctuan.danhbaso.data.AppData
import com.quoctuan.danhbaso.data.CallGroup
import com.quoctuan.danhbaso.data.CallLogRepo
import com.quoctuan.danhbaso.util.Actions
import com.quoctuan.danhbaso.util.PhoneUtil

/** Màn hình chính – Gần đây: mỗi số 1 dòng (lần gần nhất) + trạng thái có màu. */
@Composable
fun RecentsScreen(
    data: AppData,
    hasCallLog: Boolean,
    onMenu: () -> Unit,
    onSearch: () -> Unit,
    onOpen: (String) -> Unit,
    onBackup: () -> Unit,
    onGrant: () -> Unit,
) {
    val ctx = LocalContext.current
    var missedOnly by rememberSaveable { mutableStateOf(false) }
    val groups = remember(data.calls) { CallLogRepo.group(data.calls) }
    val missedCount = remember(groups) { groups.count { statusOf(it.latest) == CallStatus.MISSED } }
    val shown = remember(groups, missedOnly) {
        if (missedOnly) groups.filter { statusOf(it.latest) == CallStatus.MISSED } else groups
    }
    val buckets = remember(shown) { shown.groupBy { dayBucket(it.latest.date) } }

    Column(Modifier.fillMaxSize()) {
        // Ô tìm kiếm + nút ☰
        Row(
            Modifier.padding(16.dp, 12.dp, 16.dp, 8.dp).fillMaxWidth().height(56.dp)
                .clip(RoundedCornerShape(28.dp)).background(C.Search).glowBorder(RoundedCornerShape(28.dp)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onMenu) { Icon(Icons.Filled.Menu, "Mở menu", tint = C.Sub2) }
            Text(
                "Tìm người liên hệ", color = C.Sub, fontSize = 16.sp,
                modifier = Modifier.weight(1f).clickable(onClick = onSearch).padding(vertical = 16.dp)
            )
            IconButton(onClick = onSearch) { Icon(Icons.Filled.Search, "Tìm", tint = C.Sub2) }
        }
        // Nút lọc
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip("Tất cả", !missedOnly) { missedOnly = false }
            FilterChip("Gọi nhỡ" + if (missedCount > 0) " $missedCount" else "", missedOnly) { missedOnly = true }
        }
        // Lời nhắc sao lưu
        val days = data.daysSinceBackup()
        if (data.loaded && data.contacts.isNotEmpty() && (days == null || days >= 30)) {
            Row(
                Modifier.padding(16.dp, 10.dp, 16.dp, 0.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp))
                    .background(C.OrangeBg).padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.ErrorOutline, null, tint = C.Orange)
                Spacer(Modifier.width(10.dp))
                Text(
                    if (days == null) "Bạn chưa sao lưu danh bạ lần nào" else "Đã $days ngày chưa sao lưu danh bạ",
                    color = C.OrangeText, fontSize = 13.sp, modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = onBackup, shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = C.Orange, contentColor = C.OnOrange),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) { Text("Sao lưu ngay", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            }
        }

        if (!hasCallLog) {
            Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Chưa có quyền xem nhật ký cuộc gọi", color = C.Text, fontSize = 16.sp)
                Gap(6)
                Text(
                    "Nếu quyền bị khóa xám: vào Thông tin ứng dụng → nút ⋮ → Cho phép cài đặt hạn chế.",
                    color = C.Sub, fontSize = 13.sp
                )
                Gap(12)
                Button(onClick = onGrant) { Text("Cấp quyền") }
            }
        } else {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (shown.isEmpty() && data.loaded) {
                item { Text("Chưa có cuộc gọi nào", color = C.Sub, modifier = Modifier.padding(16.dp)) }
            }
            for ((bucket, title) in listOf(0 to "Hôm nay", 1 to "Hôm qua", 2 to "Cũ hơn")) {
                val list = buckets[bucket] ?: continue
                item(key = "h$bucket") { SectionTitle(title) }
                items(list, key = { it.key }) { g -> CallRow(data, g, onClick = { onOpen(g.number) }, onCall = { Actions.call(ctx, g.number) }) }
            }
        }
        }
    }
}


@Composable
private fun FilterChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.height(38.dp).then(if (selected) Modifier.glow(RoundedCornerShape(18.dp), 8.dp) else Modifier).clip(RoundedCornerShape(18.dp))
            .then(if (selected) Modifier.background(C.accent) else Modifier.border(1.dp, C.Border, RoundedCornerShape(18.dp)))
            .clickable(onClick = onClick).padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) { Text(text, color = if (selected) C.OnBlue else C.Sub2, fontSize = 15.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) }
}

@Composable
private fun CallRow(data: AppData, g: CallGroup, onClick: () -> Unit, onCall: () -> Unit) {
    val contact = remember(g, data.contacts) { data.contactFor(g.number) }
    val st = statusOf(g.latest)
    val meta = remember(g) {
        val dur = formatDuration(g.latest.duration)
        (if (dur.isNotEmpty()) " · $dur" else "") + " · " + formatTime(g.latest.date)
    }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(C.Card)
            .glowBorder(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick).padding(start = 14.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(contact?.name, letter = contact?.letter)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    contact?.name ?: PhoneUtil.pretty(g.number), color = C.Text, fontSize = 17.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false)
                )
                if (g.count > 1) Text(" (${g.count})", color = C.Sub, fontSize = 17.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(st.color))
                Spacer(Modifier.width(6.dp))
                Text(st.label, color = st.color, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
                Text(meta, color = C.Sub, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        IconButton(onClick = onCall) { Icon(Icons.Filled.Call, "Gọi", tint = C.Sub2) }
    }
}
