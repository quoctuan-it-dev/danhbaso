package com.quoctuan.danhbaso.call

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Animatable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quoctuan.danhbaso.ui.avatarColor
import kotlin.math.roundToInt

// Giao diện màn hình gọi. File này KHÔNG dùng android.telecom.Call để xem được trong Preview
// (Preview chạy trên máy tính, không có phần cuộc gọi của Android).

/** Một dòng trong màn gọi nhóm. */
internal data class GroupRow(val name: String, val status: GroupCall.St)

// Màu riêng của màn hình gọi (nền tối, không đổi theo bộ màu app để luôn dễ nhìn)
internal val NAVY_TOP = Color(0xFF0A1F42)
internal val NAVY_BOT = Color(0xFF06142B)
internal val GLOW_BLUE = Color(0xFF2F6FE0)
internal val GLOW_CYAN = Color(0xFF1FB5E8)
internal val GLOW_PINK = Color(0xFF8E2A73)
internal val MINT = Color(0xFF7CF5C0)
internal val SKY = Color(0xFF5CC2FF)
internal val RED_A = Color(0xFFFF6B6F)
internal val RED_B = Color(0xFFD7263D)
internal val GREEN_A = Color(0xFF55E39A)
internal val GREEN_B = Color(0xFF1AA95E)
internal val GLASS = Color.White.copy(alpha = 0.08f)
internal val GLASS_LINE = Color.White.copy(alpha = 0.14f)

/** Giao diện cuộc gọi đến (không phụ thuộc cuộc gọi thật → xem được trong Preview). */
@Composable
internal fun IncomingContent(
    saved: String?, number: String, sim: String?, waiting: Boolean,
    onAnswer: () -> Unit, onDecline: () -> Unit, onSpeaker: () -> Unit, onSmsReply: (String) -> Unit,
) {
    val name = saved ?: if (number.isBlank()) "Số ẩn" else prettyNumber(number)
    var showSms by remember { mutableStateOf(false) }
    var dragging by remember { mutableIntStateOf(0) } // 1 = đang kéo nút xanh tới giữa, -1 = nút đỏ

    Column(Modifier.fillMaxSize().padding(horizontal = 22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(20.dp))
        StatusChip((if (waiting) "Cuộc gọi chờ" else "Cuộc gọi đến") + (sim?.let { " · $it" } ?: ""))
        Spacer(Modifier.height(44.dp))
        CallAvatar(saved, dia = 136, waves = true)
        Spacer(Modifier.height(10.dp))
        Text(name, color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(4.dp))
        Text(
            if (saved != null) prettyNumber(number) else if (number.isBlank()) "" else "Số chưa lưu",
            color = Color.White.copy(alpha = 0.75f), fontSize = 16.sp
        )
        if (waiting) {
            Spacer(Modifier.height(10.dp))
            Text("Nghe máy sẽ giữ máy cuộc gọi hiện tại", color = MINT, fontSize = 13.sp)
        }
        Spacer(Modifier.weight(1f))
        SlideToAnswer(onAnswer = onAnswer, onDecline = onDecline, onDragging = { dragging = it })
        Spacer(Modifier.height(12.dp))
        // Hướng dẫn 1 dòng (cỡ chữ theo ☰ → Cỡ chữ của app)
        val hint = when (dragging) {
            1 -> androidx.compose.ui.text.AnnotatedString("Thả tay để nghe máy")
            -1 -> androidx.compose.ui.text.AnnotatedString("Thả tay để từ chối")
            else -> androidx.compose.ui.text.buildAnnotatedString {
                append("Kéo vào giữa: ")
                pushStyle(androidx.compose.ui.text.SpanStyle(color = GREEN_A, fontWeight = FontWeight.Bold)); append("xanh"); pop()
                append(" để nghe · ")
                pushStyle(androidx.compose.ui.text.SpanStyle(color = RED_A, fontWeight = FontWeight.Bold)); append("đỏ"); pop()
                append(" để từ chối")
            }
        }
        // Luôn 1 dòng: chữ to quá thì tự thu nhỏ cho vừa (không bị "…")
        androidx.compose.foundation.text.BasicText(
            hint,
            style = androidx.compose.ui.text.TextStyle(
                color = when (dragging) { 1 -> MINT; -1 -> RED_A; else -> Color.White.copy(alpha = 0.8f) },
                textAlign = TextAlign.Center
            ),
            maxLines = 1,
            autoSize = androidx.compose.foundation.text.TextAutoSize.StepBased(minFontSize = 10.sp, maxFontSize = 13.5.sp),
        )
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GlassChip(Icons.Filled.Sms, "Nhắn tin") { showSms = true }
            GlassChip(Icons.AutoMirrored.Filled.VolumeUp, "Nghe bằng loa", onSpeaker)
        }
        Spacer(Modifier.height(26.dp))
    }

    if (showSms) {
        val msgs = listOf(
            "Mình đang bận, lát mình gọi lại nhé.",
            "Mình sẽ gọi lại ngay.",
            "Đang họp, nhắn tin giúp mình nhé.",
        )
        AlertDialog(
            onDismissRequest = { showSms = false },
            title = { Text("Từ chối và nhắn tin") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (m in msgs) {
                        Text(
                            m, fontSize = 15.sp,
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                .background(Color(0x14000000))
                                .clickable { showSms = false; onSmsReply(m) }
                                .padding(12.dp)
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showSms = false }) { Text("Hủy") } }
        )
    }
}


/**
 * Thanh nghe máy cỡ lớn (cho người lớn tuổi): kéo nút XANH vào vòng giữa = nghe,
 * kéo nút ĐỎ vào vòng giữa = từ chối. Phải kéo tới giữa nên không lỡ chạm khi máy trong túi.
 */
@android.annotation.SuppressLint("UnusedBoxWithConstraintsScope") // có dùng maxWidth để tính quãng kéo
@Composable
internal fun SlideToAnswer(onAnswer: () -> Unit, onDecline: () -> Unit, onDragging: (Int) -> Unit) {
    val btn = 62.dp
    BoxWithConstraints(
        Modifier.fillMaxWidth().height(78.dp).clip(RoundedCornerShape(39.dp))
            .background(GLASS).border(1.5.dp, GLASS_LINE, RoundedCornerShape(39.dp)).padding(8.dp)
    ) {
        val density = LocalDensity.current
        // Quãng đường từ đầu thanh tới giữa
        val reach = with(density) { ((maxWidth - btn) / 2).toPx() }
        var green by remember { mutableFloatStateOf(0f) } // 0 → reach (kéo sang trái)
        var red by remember { mutableFloatStateOf(0f) }   // 0 → reach (kéo sang phải)
        var done by remember { mutableStateOf(false) }
        val greenShown by animateFloatAsState(green, label = "g")
        val redShown by animateFloatAsState(red, label = "r")
        val gp = (greenShown / reach).coerceIn(0f, 1f)
        val rp = (redShown / reach).coerceIn(0f, 1f)

        // Nút xanh và đỏ nhích nhẹ qua lại về phía giữa để gợi ý cách kéo
        val inf = rememberInfiniteTransition(label = "hint")
        val wiggle by inf.animateFloat(
            0f, 1f, infiniteRepeatable(tween(700, easing = LinearEasing), RepeatMode.Reverse), label = "w"
        )
        val idle = green == 0f && red == 0f && !done
        val nudgePx = if (idle) wiggle * with(density) { 10.dp.toPx() } else 0f

        // Vòng trắng ở giữa = đích kéo tới
        val near = maxOf(gp, rp)
        Box(
            Modifier.align(Alignment.Center).size(50.dp).scale(1f + 0.12f * near)
                .shadow((6 + 14 * near).dp, CircleShape)
                .clip(CircleShape).background(Color.White.copy(alpha = 0.92f)),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Filled.Call, null, tint = NAVY_TOP, modifier = Modifier.size(22.dp)) }

        val greenState = rememberDraggableState { d ->
            if (!done) { green = (green - d).coerceIn(0f, reach); onDragging(if (green > reach * 0.8f) 1 else 0) }
        }
        val redState = rememberDraggableState { d ->
            if (!done) { red = (red + d).coerceIn(0f, reach); onDragging(if (red > reach * 0.8f) -1 else 0) }
        }
        fun dragMod(isGreen: Boolean) = Modifier.draggable(
            state = if (isGreen) greenState else redState,
            orientation = Orientation.Horizontal,
            onDragStopped = {
                when {
                    isGreen && green > reach * 0.8f -> { done = true; green = reach; onAnswer() }
                    !isGreen && red > reach * 0.8f -> { done = true; red = reach; onDecline() }
                    else -> { green = 0f; red = 0f }
                }
                onDragging(0)
            }
        )

        // Nút ĐỎ (bên trái)
        Box(
            Modifier.align(Alignment.CenterStart)
                .offset { IntOffset((redShown + nudgePx).roundToInt(), 0) }
                .size(btn).scale(1f + 0.12f * rp)
                .shadow((14 + 16 * rp).dp, CircleShape, ambientColor = RED_B, spotColor = RED_B)
                .clip(CircleShape).background(Brush.linearGradient(listOf(RED_A, RED_B)))
                .then(dragMod(false)),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Filled.CallEnd, "Từ chối", tint = Color.White, modifier = Modifier.size(28.dp)) }

        // Nút XANH (bên phải)
        Box(
            Modifier.align(Alignment.CenterEnd)
                .offset { IntOffset(-(greenShown + nudgePx).roundToInt(), 0) }
                .size(btn).scale(1f + 0.12f * gp)
                .shadow((14 + 16 * gp).dp, CircleShape, ambientColor = GREEN_B, spotColor = GREEN_B)
                .clip(CircleShape).background(Brush.linearGradient(listOf(GREEN_A, GREEN_B)))
                .then(dragMod(true)),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Filled.Call, "Nghe", tint = Color.White, modifier = Modifier.size(28.dp)) }
    }
}


/** Giao diện đang gọi (không phụ thuộc cuộc gọi thật → xem được trong Preview). */
@Composable
internal fun OngoingContent(
    saved: String?, number: String, status: String, active: Boolean, held: Boolean,
    muted: Boolean, speaker: Boolean, otherBar: (@Composable () -> Unit)?,
    onMute: () -> Unit, onSpeaker: () -> Unit, onHold: () -> Unit, onDtmf: (Char) -> Unit,
    onAddPerson: () -> Unit, onNote: () -> Unit, onContacts: () -> Unit, onHangup: () -> Unit,
) {
    val name = saved ?: if (number.isBlank()) "Số ẩn" else prettyNumber(number)
    var keypad by remember { mutableStateOf(false) }
    var typed by remember { mutableStateOf("") }
    // Bảng nút trượt từ dưới lên khi vừa vào màn (chuyển mượt lúc nghe máy)
    val rise = remember { Animatable(1f) }
    LaunchedEffect(Unit) { rise.animateTo(0f, tween(520, easing = FastOutSlowInEasing)) }

    Column(Modifier.fillMaxSize().padding(horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(16.dp))
        StatusChip(if (active) "Đang gọi" else status)
        if (otherBar != null) {
            Spacer(Modifier.height(10.dp))
            Box(Modifier.padding(horizontal = 8.dp)) { otherBar() }
        }
        Spacer(Modifier.height(if (otherBar != null || keypad) 12.dp else 26.dp))
        // Mở bàn phím thì ảnh đại diện nhỏ lại cho đủ chỗ
        val dia by animateDpAsState(if (keypad) 64.dp else 96.dp, tween(300), label = "dia")
        CallAvatar(saved, dia = dia.value.toInt(), waves = false)
        Spacer(Modifier.height(4.dp))
        Text(name, color = Color.White, fontSize = if (keypad) 20.sp else 25.sp, fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (saved != null) {
            Text(prettyNumber(number), color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
        }
        // Đồng hồ / trạng thái: nằm dưới số điện thoại, chữ nhỏ
        Spacer(Modifier.height(4.dp))
        Text(status, color = if (active) MINT else Color.White.copy(alpha = 0.8f), fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
        Spacer(Modifier.weight(1f))

        // Tấm kính chứa các nút: rộng gần hết màn, đặt cao hơn đáy
        Column(
            Modifier.fillMaxWidth()
                .graphicsLayer { translationY = rise.value * 160.dp.toPx(); alpha = 1f - rise.value * 0.6f }
                .clip(RoundedCornerShape(30.dp)).background(GLASS)
                .border(1.dp, GLASS_LINE, RoundedCornerShape(30.dp)).padding(horizontal = 4.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (keypad) {
                Text(typed.ifEmpty { " " }, color = Color.White, fontSize = 24.sp, letterSpacing = 2.sp, maxLines = 1)
                Spacer(Modifier.height(8.dp))
                val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "*", "0", "#")
                keys.chunked(3).forEach { row ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                        row.forEach { k ->
                            Box(
                                Modifier.size(58.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.1f))
                                    .clickable {
                                        typed += k
                                        onDtmf(k[0])
                                    },
                                contentAlignment = Alignment.Center
                            ) { Text(k, color = Color.White, fontSize = 24.sp) }
                        }
                    }
                }
                TextButton(onClick = { keypad = false }) { Text("Ẩn bàn phím", color = SKY) }
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    GlassButton(Icons.Filled.MicOff, "Tắt tiếng", muted, w = 80.dp, onClick = onMute)
                    GlassButton(Icons.Filled.Dialpad, "Bàn phím", false, w = 80.dp) { keypad = true }
                    GlassButton(Icons.AutoMirrored.Filled.VolumeUp, "Loa ngoài", speaker, w = 80.dp, onClick = onSpeaker)
                    GlassButton(Icons.Filled.Pause, if (held) "Tiếp tục" else "Giữ máy", held, w = 80.dp, onClick = onHold)
                }
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    GlassButton(Icons.Filled.PersonAdd, "Thêm người", false, w = 92.dp, onClick = onAddPerson)
                    GlassButton(
                        Icons.Filled.EditNote, "Ghi số", false, w = 92.dp,
                        tint = Brush.linearGradient(listOf(Color(0xFFFFD27A), Color(0xFFF2A33A))), onClick = onNote
                    )
                    GlassButton(Icons.Filled.Contacts, "Danh bạ", false, w = 92.dp, onClick = onContacts)
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(
                Modifier.width(180.dp).height(56.dp)
                    .shadow(16.dp, RoundedCornerShape(28.dp), ambientColor = RED_B, spotColor = RED_B)
                    .clip(RoundedCornerShape(28.dp)).background(Brush.linearGradient(listOf(RED_A, RED_B)))
                    .clickable(onClick = onHangup),
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.CallEnd, null, tint = Color.White, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Text("Kết thúc", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(40.dp))
    }
}


/** Giao diện gọi nhóm (không phụ thuộc cuộc gọi thật → xem được trong Preview). */
@Composable
internal fun GroupCallContent(
    title: String, members: List<GroupRow>, timer: String?, muted: Boolean, speaker: Boolean,
    onMute: () -> Unit, onSpeaker: () -> Unit, onAddPerson: () -> Unit,
    onRetry: (Int) -> Unit, onKick: (Int) -> Unit, onEnd: () -> Unit,
) {
    val joined = members.count { it.status == GroupCall.St.JOINED }
    val inviting = members.count { it.status == GroupCall.St.WAIT || it.status == GroupCall.St.RINGING }

    Column(Modifier.fillMaxSize().padding(horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(20.dp))
        StatusChip(if (joined > 0) "Gọi nhóm · $joined người" else "Đang gọi nhóm")
        Spacer(Modifier.height(18.dp))
        Text(
            if (joined == 0 && inviting > 0) "Đang mời $inviting người…" else title,
            color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center
        )
        if (joined > 0 && timer != null) {
            Spacer(Modifier.height(4.dp))
            Text(timer, color = MINT, fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }
        Spacer(Modifier.height(16.dp))
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(GLASS)
                .border(1.dp, GLASS_LINE, RoundedCornerShape(22.dp)).padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            members.forEachIndexed { i, m ->
                Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    val letter = m.name.trim().firstOrNull { it.isLetter() }?.uppercase()
                    Box(
                        Modifier.size(40.dp).clip(CircleShape)
                            .background(if (letter != null) avatarColor(m.name) else Color(0xFF2A5D9A)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (letter != null) Text(letter, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        else Icon(Icons.Filled.Person, null, tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(m.name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1,
                        overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    val (txt, col) = when (m.status) {
                        GroupCall.St.JOINED -> "Đang nói" to MINT
                        GroupCall.St.RINGING -> "Đang đổ chuông…" to SKY
                        GroupCall.St.WAIT -> "Chờ tới lượt" to Color.White.copy(alpha = 0.7f)
                        GroupCall.St.MISSED -> "Không nghe · Gọi lại" to RED_A
                        GroupCall.St.LEFT -> "Đã rời · Gọi lại" to Color(0xFFFFB35C)
                    }
                    val canRetry = m.status == GroupCall.St.MISSED || m.status == GroupCall.St.LEFT
                    Text(
                        txt, color = col, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(col.copy(alpha = 0.16f))
                            .then(if (canRetry) Modifier.clickable { onRetry(i) } else Modifier)
                            .padding(horizontal = 9.dp, vertical = 4.dp)
                    )
                    if (m.status == GroupCall.St.JOINED) {
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            Icons.Filled.Close, "Cho rời nhóm", tint = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(32.dp).clip(CircleShape).clickable { onKick(i) }.padding(6.dp)
                        )
                    }
                }
                if (i < members.lastIndex) Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.08f)))
            }
        }
        if (joined == 0) {
            Spacer(Modifier.height(12.dp))
            Text(
                "App tự gọi lần lượt từng người. Ai nghe máy thì tự gộp vào. Không nghe sau 30 giây thì gọi người kế.",
                color = Color.White.copy(alpha = 0.7f), fontSize = 12.5.sp, textAlign = TextAlign.Center
            )
        }
        Spacer(Modifier.weight(1f))
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(30.dp)).background(GLASS)
                .border(1.dp, GLASS_LINE, RoundedCornerShape(30.dp)).padding(horizontal = 4.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (joined > 0) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    GlassButton(Icons.Filled.MicOff, "Tắt tiếng", muted, onClick = onMute)
                    GlassButton(Icons.AutoMirrored.Filled.VolumeUp, "Loa ngoài", speaker, onClick = onSpeaker)
                    GlassButton(Icons.Filled.PersonAdd, "Thêm người", false, onClick = onAddPerson)
                }
                Spacer(Modifier.height(16.dp))
            }
            Row(
                Modifier.width(190.dp).height(56.dp)
                    .shadow(16.dp, RoundedCornerShape(28.dp), ambientColor = RED_B, spotColor = RED_B)
                    .clip(RoundedCornerShape(28.dp)).background(Brush.linearGradient(listOf(RED_A, RED_B)))
                    .clickable(onClick = onEnd),
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.CallEnd, null, tint = Color.White, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (joined > 0) "Kết thúc cả nhóm" else "Hủy gọi nhóm", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(40.dp))
    }
}


// ───────────────────────── Thành phần dùng chung ─────────────────────────

@Composable
internal fun StatusChip(text: String) {
    Row(
        Modifier.clip(RoundedCornerShape(16.dp)).background(GLASS).border(1.dp, GLASS_LINE, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(8.dp).shadow(6.dp, CircleShape, ambientColor = MINT, spotColor = MINT).clip(CircleShape).background(MINT))
        Spacer(Modifier.width(8.dp))
        Text(text, color = Color.White.copy(alpha = 0.92f), fontSize = 13.sp)
    }
}

/** Ảnh đại diện chữ cái, viền sáng nhiều màu; [waves] = vòng sóng tỏa ra khi đổ chuông. */
@Composable
internal fun CallAvatar(name: String?, dia: Int, waves: Boolean) {
    val inf = rememberInfiniteTransition(label = "av")
    val w by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(1800, easing = LinearEasing)), label = "w")
    val rot by inf.animateFloat(0f, 360f, infiniteRepeatable(tween(6000, easing = LinearEasing)), label = "rot")
    // Số chưa lưu: hiện hình người (không hiện chữ số "0")
    val letter = name?.trim()?.firstOrNull { it.isLetter() }?.uppercase()
    val base = if (letter == null) Color(0xFF2A5D9A) else avatarColor(name)
    Box(Modifier.size((dia + 36).dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val r = dia.dp.toPx() / 2
            if (waves) {
                for (k in 0..1) {
                    val f = (w + k * 0.5f) % 1f
                    drawCircle(SKY.copy(alpha = 0.45f * (1f - f)), r + 6.dp.toPx() + f * 11.dp.toPx(), style = Stroke(1.5.dp.toPx()))
                }
            }
            // Viền sáng xoay
            val ring = Brush.sweepGradient(listOf(SKY, MINT, Color(0xFFB46CFF), SKY), center)
            rotate(rot) { drawCircle(ring, r + 4.dp.toPx(), style = Stroke(3.dp.toPx())) }
        }
        Box(
            Modifier.size((dia - 8).dp).shadow(18.dp, CircleShape).clip(CircleShape)
                .background(Brush.linearGradient(listOf(base.copy(alpha = 0.85f), base))),
            contentAlignment = Alignment.Center
        ) {
            if (letter != null) Text(letter, color = Color.White, fontSize = (dia * 0.38f).sp, fontWeight = FontWeight.Bold)
            else Icon(Icons.Filled.Person, null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size((dia * 0.5f).dp))
        }
    }
}

/** Nút vuông bo góc dạng kính mờ; đang bật thì phát sáng xanh. */
@Composable
internal fun GlassButton(
    icon: ImageVector, label: String, on: Boolean,
    w: androidx.compose.ui.unit.Dp = 96.dp, tint: Brush? = null, onClick: () -> Unit,
) {
    Column(Modifier.width(w), horizontalAlignment = Alignment.CenterHorizontally) {
        val bg = when {
            on -> Brush.linearGradient(listOf(SKY, Color(0xFF2F86FF)))
            tint != null -> tint
            else -> Brush.linearGradient(listOf(GLASS, GLASS))
        }
        Box(
            Modifier.size(58.dp)
                .then(if (on) Modifier.shadow(14.dp, RoundedCornerShape(20.dp), ambientColor = SKY, spotColor = SKY) else Modifier)
                .clip(RoundedCornerShape(20.dp))
                .background(bg)
                .border(1.dp, if (on || tint != null) Color.Transparent else GLASS_LINE, RoundedCornerShape(20.dp))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) { Icon(icon, label, tint = Color.White, modifier = Modifier.size(26.dp)) }
        Spacer(Modifier.height(6.dp))
        Text(label, color = Color.White.copy(alpha = 0.92f), fontSize = 12.5.sp, maxLines = 1)
    }
}

@Composable
internal fun GlassChip(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(16.dp)).background(GLASS).border(1.dp, GLASS_LINE, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(8.dp))
        // Luôn 1 dòng, chữ tự thu nhỏ nếu chật
        androidx.compose.foundation.text.BasicText(
            label,
            style = androidx.compose.ui.text.TextStyle(color = Color.White, fontWeight = FontWeight.SemiBold),
            maxLines = 1,
            autoSize = androidx.compose.foundation.text.TextAutoSize.StepBased(minFontSize = 11.sp, maxFontSize = 14.sp),
        )
    }
}

@Composable
internal fun SmallGlass(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.12f))
            .clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, color = Color.White, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
    }
}

internal fun formatDuration(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%02d:%02d".format(m, sec)
}

/** 0917600108 → 0917 600 108 */
internal fun prettyNumber(n: String): String = try {
    com.quoctuan.danhbaso.util.PhoneUtil.pretty(n)
} catch (e: Exception) {
    n
}

/** Nền tối có vệt sáng loang màu của màn hình gọi. */
internal fun Modifier.callBackground(): Modifier = this
    .background(Brush.verticalGradient(listOf(NAVY_TOP, NAVY_BOT)))
    .drawBehind {
        drawCircle(Brush.radialGradient(listOf(GLOW_BLUE.copy(alpha = 0.55f), Color.Transparent), Offset(size.width * 0.2f, size.height * 0.18f), size.width * 0.75f), size.width * 0.75f, Offset(size.width * 0.2f, size.height * 0.18f))
        drawCircle(Brush.radialGradient(listOf(GLOW_CYAN.copy(alpha = 0.45f), Color.Transparent), Offset(size.width * 0.9f, size.height * 0.38f), size.width * 0.65f), size.width * 0.65f, Offset(size.width * 0.9f, size.height * 0.38f))
        drawCircle(Brush.radialGradient(listOf(GLOW_PINK.copy(alpha = 0.4f), Color.Transparent), Offset(size.width * 0.5f, size.height * 1.02f), size.width * 0.8f), size.width * 0.8f, Offset(size.width * 0.5f, size.height * 1.02f))
    }


// ───────────────────────── Ghi số khi đang gọi ─────────────────────────

/** Số vừa ghi đã có trong danh bạ: [name] = tên đã lưu (null khi máy khóa → không lộ tên). */
internal data class NoteDup(val name: String?)

/** Thanh nhỏ phía trên các màn phụ: tên người đang gọi + đồng hồ, để biết cuộc gọi vẫn chạy. */
@Composable
internal fun MiniCallHeader(callerName: String, timer: String) {
    Row(
        Modifier.clip(RoundedCornerShape(18.dp)).background(Color.White.copy(alpha = 0.07f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(MINT))
        Spacer(Modifier.width(8.dp))
        Text(callerName, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.width(8.dp))
        Text(timer, color = MINT, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

/**
 * Ghi số người kia đọc trong lúc đang gọi. Bàn phím ở đây gõ IM LẶNG (không phát tiếng "tít").
 * Bước 1: gõ số. Bước 2: gõ tên, chọn Gmail / SIM, bấm Lưu số.
 * Khi máy khóa: không có nút Sao chép, và nếu số đã có thì không hiện tên đã lưu.
 */
@Composable
internal fun NoteSheet(
    callerName: String, timer: String, locked: Boolean, hasSim: Boolean,
    findExisting: (String) -> NoteDup?,
    onSave: (name: String, number: String, gmail: Boolean, sim: Boolean) -> Unit,
    onCopy: (String) -> Unit,
    onClose: (unsavedNumber: String) -> Unit,
) {
    var number by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var step by remember { mutableIntStateOf(1) }
    var gmail by remember { mutableStateOf(true) }
    var sim by remember { mutableStateOf(hasSim) }
    val dup = remember(number, step) { if (step == 2 && number.length >= 3) findExisting(number) else null }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(16.dp))
        MiniCallHeader(callerName, timer)
        Spacer(Modifier.height(14.dp))
        // Ô số
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color.White.copy(alpha = 0.08f))
                .border(1.dp, GLASS_LINE, RoundedCornerShape(20.dp)).padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(if (step == 1) "Số vừa nghe" else "Số", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (number.isEmpty()) "…" else prettyNumber(number), color = Color.White, fontSize = 28.sp,
                    fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp, maxLines = 1, modifier = Modifier.weight(1f)
                )
                if (step == 1) {
                    // ⌫: bấm = xóa 1 số, giữ lâu = xóa hết
                    Icon(
                        Icons.AutoMirrored.Filled.Backspace, "Xóa", tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(44.dp).clip(CircleShape)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onTap = { number = number.dropLast(1) },
                                    onLongPress = { number = "" }
                                )
                            }.padding(10.dp)
                    )
                } else {
                    Icon(
                        Icons.Filled.Edit, "Sửa số", tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(44.dp).clip(CircleShape).clickable { step = 1 }.padding(10.dp)
                    )
                }
            }
            if (step == 2) {
                Spacer(Modifier.height(8.dp))
                Text("Tên (không bắt buộc)", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                Box(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                    if (name.isEmpty()) Text("VD: Chú Năm sửa điện", color = Color.White.copy(alpha = 0.35f), fontSize = 17.sp)
                    androidx.compose.foundation.text.BasicTextField(
                        value = name, onValueChange = { name = it }, singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 17.sp),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(MINT),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Box(Modifier.fillMaxWidth().padding(top = 4.dp).height(1.5.dp).background(MINT))
            }
        }

        if (step == 1) {
            Spacer(Modifier.height(12.dp))
            val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "*", "0", "#")
            keys.chunked(3).forEach { row ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    row.forEach { k ->
                        Box(
                            Modifier.width(92.dp).height(54.dp).clip(RoundedCornerShape(27.dp))
                                .background(Color.White.copy(alpha = 0.1f))
                                .clickable { if (number.length < 15) number += k },
                            contentAlignment = Alignment.Center
                        ) { Text(k, color = Color.White, fontSize = 24.sp) }
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text("Bấm số ở đây KHÔNG phát tiếng sang người kia", color = MINT, fontSize = 12.sp)
        } else {
            Spacer(Modifier.height(12.dp))
            // Lưu vào: Gmail / SIM (chọn 1 hoặc cả 2)
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color.White.copy(alpha = 0.08f))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text("Lưu vào (chọn 1 hoặc cả 2)", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    NoteCheck("Gmail", gmail) { gmail = !gmail }
                    Spacer(Modifier.width(18.dp))
                    NoteCheck(if (hasSim) "SIM" else "SIM (không thấy SIM)", sim && hasSim) { if (hasSim) sim = !sim }
                }
            }
            if (dup != null) {
                Spacer(Modifier.height(10.dp))
                Text(
                    if (dup.name != null) "Số này đã lưu tên \"${dup.name}\"" else "Số này đã có trong danh bạ",
                    color = Color(0xFFFFB35C), fontSize = 13.sp, fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (step == 1) {
                NoteBtn("Đóng", Modifier.weight(1f), primary = false) { onClose(number) }
                NoteBtn("Tiếp ›", Modifier.weight(1f), primary = true, enabled = number.length >= 3) { step = 2 }
            } else {
                if (!locked) NoteBtn("Sao chép", Modifier.weight(1f), primary = false) { onCopy(number) }
                else NoteBtn("Đóng", Modifier.weight(1f), primary = false) { onClose(number) }
                NoteBtn("Lưu số", Modifier.weight(1f), primary = true, enabled = gmail || (sim && hasSim)) {
                    onSave(name.trim(), number, gmail, sim && hasSim)
                }
            }
        }
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun NoteCheck(label: String, on: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(10.dp)).clickable(onClick = onClick).padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(22.dp).clip(RoundedCornerShape(6.dp))
                .background(if (on) Color(0xFF2FC46E) else Color.Transparent)
                .border(2.dp, if (on) Color(0xFF2FC46E) else Color.White.copy(alpha = 0.5f), RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) { if (on) Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(16.dp)) }
        Spacer(Modifier.width(8.dp))
        Text(label, color = Color.White, fontSize = 15.sp)
    }
}

@Composable
private fun NoteBtn(text: String, modifier: Modifier, primary: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        modifier.height(52.dp).clip(RoundedCornerShape(26.dp))
            .background(
                if (!enabled) Brush.linearGradient(listOf(Color.White.copy(alpha = 0.08f), Color.White.copy(alpha = 0.08f)))
                else if (primary) Brush.linearGradient(listOf(GREEN_A, GREEN_B))
                else Brush.linearGradient(listOf(Color.White.copy(alpha = 0.1f), Color.White.copy(alpha = 0.1f)))
            )
            .border(1.dp, if (primary) Color.Transparent else GLASS_LINE, RoundedCornerShape(26.dp))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Text(text, color = Color.White.copy(alpha = if (enabled) 1f else 0.4f), fontSize = 15.sp, fontWeight = FontWeight.Bold) }
}

// ───────────────────────── Danh bạ trong màn gọi ─────────────────────────

/** Xem số trong danh bạ mà không rời màn gọi: tìm tên, số hiện to để đọc, Sao chép, Gửi SMS. */
@Composable
internal fun InCallContactsSheet(
    callerName: String, timer: String, contacts: List<Pair<String, String>>,
    onCopy: (String) -> Unit, onSms: (name: String, number: String) -> Unit, onBack: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var picked by remember { mutableStateOf<Pair<String, String>?>(null) }
    val shown = remember(contacts, query) {
        if (query.isBlank()) contacts else {
            val q = com.quoctuan.danhbaso.util.TextUtil.removeDiacritics(query).lowercase().trim()
            val qd = query.filter { it.isDigit() }
            contacts.filter {
                com.quoctuan.danhbaso.util.TextUtil.removeDiacritics(it.first).lowercase().contains(q) ||
                    (qd.isNotEmpty() && it.second.filter { c -> c.isDigit() }.contains(qd))
            }
        }
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(16.dp))
        MiniCallHeader(callerName, timer)
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(24.dp)).background(Color.White.copy(alpha = 0.08f))
                .border(1.dp, GLASS_LINE, RoundedCornerShape(24.dp)).padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Search, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Box(Modifier.weight(1f)) {
                if (query.isEmpty()) Text("Tìm tên hoặc số…", color = Color.White.copy(alpha = 0.45f), fontSize = 15.sp)
                androidx.compose.foundation.text.BasicTextField(
                    value = query, onValueChange = { query = it; picked = null }, singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 15.sp),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(MINT), modifier = Modifier.fillMaxWidth()
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        androidx.compose.foundation.lazy.LazyColumn(
            Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color.White.copy(alpha = 0.06f))
        ) {
            items(shown.size) { i ->
                val c = shown[i]
                val open = picked == c
                Column(
                    Modifier.fillMaxWidth().clickable { picked = if (open) null else c }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val letter = c.first.trim().firstOrNull { it.isLetter() }?.uppercase()
                        Box(
                            Modifier.size(34.dp).clip(CircleShape)
                                .background(if (letter != null) com.quoctuan.danhbaso.ui.avatarColor(c.first) else Color(0xFF2A5D9A)),
                            contentAlignment = Alignment.Center
                        ) { Text(letter ?: "#", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
                        Spacer(Modifier.width(10.dp))
                        Text(c.first, color = Color.White, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        if (!open) Text(prettyNumber(c.second), color = Color.White.copy(alpha = 0.55f), fontSize = 12.5.sp)
                    }
                    if (open) {
                        // Số hiện to để đọc cho người kia
                        Text(prettyNumber(c.second), color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp, modifier = Modifier.padding(top = 8.dp))
                        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            GlassChip(Icons.Filled.ContentCopy, "Sao chép") { onCopy(c.second) }
                            GlassChip(Icons.Filled.Sms, "Gửi SMS") { onSms(c.first, c.second) }
                        }
                    }
                }
                if (i < shown.lastIndex) Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.06f)))
            }
        }
        Spacer(Modifier.height(12.dp))
        NoteBtn("‹ Quay lại cuộc gọi", Modifier.fillMaxWidth(), primary = false, onClick = onBack)
        Spacer(Modifier.height(40.dp))
    }
}
