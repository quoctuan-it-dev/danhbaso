package com.quoctuan.danhbaso.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quoctuan.danhbaso.data.AppData
import com.quoctuan.danhbaso.util.Actions
import com.quoctuan.danhbaso.util.PhoneUtil

private val keys = listOf(
    "1" to "", "2" to "ABC", "3" to "DEF",
    "4" to "GHI", "5" to "JKL", "6" to "MNO",
    "7" to "PQRS", "8" to "TUV", "9" to "WXYZ",
    "*" to "", "0" to "+", "#" to "",
)

/** Bàn phím số: gõ vài số là hiện gợi ý tên. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DialpadScreen(data: AppData, onOpenNumber: (String) -> Unit, onSaveNumber: (String) -> Unit) {
    val ctx = LocalContext.current
    val view = androidx.compose.ui.platform.LocalView.current
    var digits by rememberSaveable { mutableStateOf("") }
    // Cỡ chữ / phím theo lựa chọn Nhỏ – Vừa – Lớn (menu ☰ → Cỡ bàn phím)
    val sz = data.keypadSize
    val keyH = listOf(54, 62, 72)[sz].dp
    val keyGap = listOf(6, 8, 8)[sz].dp
    val digitSp = listOf(28, 34, 40)[sz].sp
    val letterSp = listOf(9, 10, 11)[sz].sp
    val nameSp = listOf(18, 20, 23)[sz].sp
    val numSp = listOf(14, 16, 18)[sz].sp
    val avatarSize = listOf(44, 48, 54)[sz].dp
    val numberSp = listOf(32, 38, 44)[sz].sp

    // Gợi ý: người trong danh bạ có số chứa dãy đã gõ, rồi tới số trong nhật ký cuộc gọi
    val suggestions: List<Pair<String?, String>> = androidx.compose.runtime.remember(digits, data.visibleContacts, data.calls) { if (digits.length < 2) emptyList() else {
        val q = digits.filter { it.isDigit() }
        val fromContacts = data.visibleContacts.flatMap { c -> c.phones.map { c.name to it } }
            .filter { PhoneUtil.local(it.second).contains(q) }
            .sortedBy { if (PhoneUtil.local(it.second).startsWith(q)) 0 else 1 }
        val known = fromContacts.map { PhoneUtil.key(it.second) }.toSet()
        val fromLog = data.calls.map { it.number }.distinctBy { PhoneUtil.key(it) }
            .filter { PhoneUtil.local(it).contains(q) && PhoneUtil.key(it) !in known }
            .map { null to it }
        (fromContacts + fromLog).take(50)
    } }

    Column(Modifier.fillMaxSize()) {
        // Gợi ý: bắt đầu từ trên xuống, vuốt để xem thêm; KHÔNG đẩy dãy số ra khỏi màn hình
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(suggestions) { (name, number) ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(C.Card)
                        .clickable { if (name == null) onOpenNumber(number) else Actions.call(ctx, number) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Avatar(name, avatarSize)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(name ?: PhoneUtil.pretty(number), color = C.Text, fontSize = nameSp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(if (name == null) "Số chưa lưu" else PhoneUtil.pretty(number), color = C.Sub, fontSize = numSp)
                    }
                    Icon(Icons.Filled.Call, "Gọi", tint = C.Green, modifier = Modifier.size(26.dp))
                }
            }
        }
        // Dãy số đang gõ: luôn nằm ngay trên bàn phím
        Text(
            digits, color = C.Text,
            fontSize = if (digits.length > 13) 26.sp else numberSp, fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Clip,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
        )
        Column(
            Modifier.fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(keyGap)
        ) {
            for (row in keys.chunked(3)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    for ((n, l) in row) {
                        DialKey(n, l, keyH, digitSp, letterSp, view,
                            onClick = { digits += n },
                            onLongClick = { digits += if (n == "0") "+" else n })
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                // Nút Lưu số: to, có nền rõ ràng; mở màn Thêm số mới với số đã điền sẵn
                val on = PhoneUtil.local(digits).length >= 3
                Row(
                    Modifier.weight(1f).height(keyH).clip(RoundedCornerShape(keyH / 2))
                        .background(if (on) C.BlueBg else C.Card2)
                        .then(if (on) Modifier.glowBorder(RoundedCornerShape(keyH / 2), 1.5.dp) else Modifier)
                        .clickable(enabled = on) { onSaveNumber(digits) },
                    horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.PersonAdd, "Lưu số", tint = if (on) C.Blue else C.Sub, modifier = Modifier.size(26.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Lưu số", color = if (on) C.Blue else C.Sub, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Box(
                    Modifier.weight(1f).height(keyH).glow(RoundedCornerShape(keyH / 2), 14.dp, C.Green)
                        .clip(RoundedCornerShape(keyH / 2)).background(C.Green)
                        .clickable { if (digits.isNotEmpty()) Actions.call(ctx, digits) },
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Filled.Call, "Gọi", tint = C.OnGreen, modifier = Modifier.size(30.dp)) }
                Box(
                    Modifier.weight(1f).height(keyH).clip(RoundedCornerShape(keyH / 2))
                        .combinedClickable(onClick = { digits = digits.dropLast(1) }, onLongClick = { digits = "" }),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Filled.Backspace, "Xóa số", tint = C.Sub2, modifier = Modifier.size(28.dp)) }
            }
        }
    }
}

/** Hộp chọn cỡ bàn phím: Nhỏ / Vừa / Lớn. */
@Composable
fun KeypadSizeDialog(data: AppData, onDismiss: () -> Unit) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = C.Nav,
        title = { Text("Cỡ bàn phím và chữ", color = C.Text) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Nhỏ", "Vừa", "Lớn").forEachIndexed { i, label ->
                    val sel = data.keypadSize == i
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                            .background(if (sel) C.BlueBg else C.Card)
                            .clickable { data.changeKeypadSize(i) }.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(label, color = C.Text, fontSize = listOf(16, 19, 23)[i].sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.weight(1f))
                        Text("1 2 3", color = C.Sub2, fontSize = listOf(18, 22, 27)[i].sp)
                    }
                }
            }
        },
        confirmButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Xong") } }
    )
}

/**
 * Một phím số kiểu B: vòng tròn viền mảnh.
 * Chỉ khi ngón tay đang chạm, phím mới sáng xanh + rung nhẹ; nhả tay là trở lại bình thường.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun androidx.compose.foundation.layout.RowScope.DialKey(
    n: String, l: String,
    keyH: androidx.compose.ui.unit.Dp,
    digitSp: androidx.compose.ui.unit.TextUnit,
    letterSp: androidx.compose.ui.unit.TextUnit,
    view: android.view.View,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val src = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    androidx.compose.runtime.LaunchedEffect(pressed) {
        if (pressed) view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
    }
    val shape = RoundedCornerShape(keyH / 2)
    Column(
        Modifier.weight(1f).height(keyH)
            .then(if (pressed) Modifier.glow(shape, 14.dp) else Modifier).clip(shape)
            .background(if (pressed) C.BlueBg else Color.Transparent)
            .then(if (pressed) Modifier.border(1.5.dp, C.AccentA, shape) else Modifier.glowBorder(shape, 1.5.dp))
            .combinedClickable(
                interactionSource = src, indication = null,
                onClick = onClick, onLongClick = onLongClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(n, color = C.Text, fontSize = digitSp, fontWeight = FontWeight.Normal, lineHeight = digitSp)
        if (l.isNotEmpty()) Text(l, color = C.Sub, fontSize = letterSp, letterSpacing = 1.5.sp, lineHeight = letterSp)
    }
}
