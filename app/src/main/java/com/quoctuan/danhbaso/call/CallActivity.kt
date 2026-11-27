package com.quoctuan.danhbaso.call

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.telecom.Call
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.CallMerge
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.SwapCalls
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quoctuan.danhbaso.MainActivity
import com.quoctuan.danhbaso.ui.avatarColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

// Màu riêng của màn hình gọi (nền tối, không đổi theo bộ màu app để luôn dễ nhìn)
private val NAVY_TOP = Color(0xFF0A1F42)
private val NAVY_BOT = Color(0xFF06142B)
private val GLOW_BLUE = Color(0xFF2F6FE0)
private val GLOW_CYAN = Color(0xFF1FB5E8)
private val GLOW_PINK = Color(0xFF8E2A73)
private val MINT = Color(0xFF7CF5C0)
private val SKY = Color(0xFF5CC2FF)
private val RED_A = Color(0xFFFF6B6F)
private val RED_B = Color(0xFFD7263D)
private val GREEN_A = Color(0xFF55E39A)
private val GREEN_B = Color(0xFF1AA95E)
private val GLASS = Color.White.copy(alpha = 0.08f)
private val GLASS_LINE = Color.White.copy(alpha = 0.14f)

/** Màn hình cuộc gọi đến / đang gọi của Danh bạ số. Không qua cổng kiểm tra bảo mật để không lỡ cuộc gọi. */
class CallActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showOnLockScreen()
        enableEdgeToEdge()
        handle(intent)
        setContent { CallRoot(onClose = { finish() }) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    /** Nút "Nghe" / "Nghe bằng loa" bấm từ thông báo. */
    private fun handle(i: Intent?) {
        val todo = i?.getStringExtra(CallNotifier.EXTRA_DO) ?: return
        i.removeExtra(CallNotifier.EXTRA_DO)
        CallManager.ringing()?.let { CallManager.answer(it, speaker = todo == "answer_speaker") }
    }

    @Suppress("DEPRECATION")
    private fun showOnLockScreen() {
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}

@Composable
private fun CallRoot(onClose: () -> Unit) {
    @Suppress("UNUSED_VARIABLE") val t = CallManager.tick // vẽ lại khi trạng thái đổi
    val current = CallManager.current()
    val other = CallManager.calls.firstOrNull { it != current && CallManager.state(it) != Call.STATE_DISCONNECTED }

    // Hết cuộc gọi: hiện "Đã kết thúc" chốc lát rồi đóng
    LaunchedEffect(current == null) {
        if (current == null) {
            delay(900)
            onClose()
        }
    }

    Box(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(NAVY_TOP, NAVY_BOT)))
            .drawBehind {
                // Vệt sáng loang màu phía sau
                drawCircle(Brush.radialGradient(listOf(GLOW_BLUE.copy(alpha = 0.55f), Color.Transparent), Offset(size.width * 0.2f, size.height * 0.18f), size.width * 0.75f), size.width * 0.75f, Offset(size.width * 0.2f, size.height * 0.18f))
                drawCircle(Brush.radialGradient(listOf(GLOW_CYAN.copy(alpha = 0.45f), Color.Transparent), Offset(size.width * 0.9f, size.height * 0.38f), size.width * 0.65f), size.width * 0.65f, Offset(size.width * 0.9f, size.height * 0.38f))
                drawCircle(Brush.radialGradient(listOf(GLOW_PINK.copy(alpha = 0.4f), Color.Transparent), Offset(size.width * 0.5f, size.height * 1.02f), size.width * 0.8f), size.width * 0.8f, Offset(size.width * 0.5f, size.height * 1.02f))
            }
            .safeDrawingPadding()
    ) {
        when {
            current == null -> Text(
                "Cuộc gọi đã kết thúc", color = Color.White, fontSize = 20.sp,
                modifier = Modifier.align(Alignment.Center)
            )
            CallManager.state(current) == Call.STATE_RINGING -> IncomingScreen(current, other)
            else -> OngoingScreen(current, other)
        }
    }
}

// ───────────────────────── Cuộc gọi đến ─────────────────────────

@Composable
private fun IncomingScreen(call: Call, other: Call?) {
    val ctx = LocalContext.current
    val number = CallManager.number(call)
    val name by produceState(number.ifBlank { "Số ẩn" }, number) {
        value = withContext(Dispatchers.IO) { CallInfo.display(ctx, number) }
    }
    val sim = remember(call) { CallInfo.simLabel(ctx, call) }
    var showSms by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(horizontal = 22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(20.dp))
        StatusChip(if (other != null) "Cuộc gọi chờ" + (sim?.let { " · $it" } ?: "") else "Cuộc gọi đến" + (sim?.let { " · $it" } ?: ""))
        Spacer(Modifier.height(44.dp))
        CallAvatar(name, dia = 128, waves = true)
        Spacer(Modifier.height(30.dp))
        Text(name, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (name != number && number.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(prettyNumber(number), color = Color.White.copy(alpha = 0.75f), fontSize = 15.sp)
        }
        if (other != null) {
            Spacer(Modifier.height(10.dp))
            Text("Nghe máy sẽ giữ máy cuộc gọi hiện tại", color = MINT, fontSize = 13.sp)
        }
        Spacer(Modifier.weight(1f))
        SlideToAnswer(onAnswer = { CallManager.answer(call) }, onDecline = { CallManager.decline(call) })
        Spacer(Modifier.height(8.dp))
        Text("Vuốt sang phải để nghe · sang trái để từ chối", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GlassChip(Icons.Filled.Sms, "Nhắn tin") { showSms = true }
            GlassChip(Icons.Filled.VolumeUp, "Nghe bằng loa") { CallManager.answer(call, speaker = true) }
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
                                .clickable { showSms = false; CallManager.declineWithText(call, m) }
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

/** Thanh vuốt: kéo nút giữa sang phải = nghe, sang trái = từ chối. Không lỡ chạm khi máy trong túi. */
@Composable
private fun SlideToAnswer(onAnswer: () -> Unit, onDecline: () -> Unit) {
    BoxWithConstraints(
        Modifier.fillMaxWidth().height(72.dp).clip(RoundedCornerShape(36.dp))
            .background(GLASS).border(1.dp, GLASS_LINE, RoundedCornerShape(36.dp)).padding(5.dp)
    ) {
        val density = LocalDensity.current
        val max = with(density) { ((maxWidth - 62.dp) / 2).toPx() }
        var drag by remember { mutableFloatStateOf(0f) }
        var done by remember { mutableStateOf(false) }
        val shown by animateFloatAsState(drag, label = "slide")
        val p = (shown / max).coerceIn(-1f, 1f)

        Box(
            Modifier.align(Alignment.CenterStart).size(62.dp).scale(1f + 0.15f * (-p).coerceAtLeast(0f))
                .shadow(14.dp, CircleShape, ambientColor = RED_B, spotColor = RED_B)
                .clip(CircleShape).background(Brush.linearGradient(listOf(RED_A, RED_B))),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Filled.CallEnd, "Từ chối", tint = Color.White, modifier = Modifier.size(28.dp)) }
        Box(
            Modifier.align(Alignment.CenterEnd).size(62.dp).scale(1f + 0.15f * p.coerceAtLeast(0f))
                .shadow(14.dp, CircleShape, ambientColor = GREEN_B, spotColor = GREEN_B)
                .clip(CircleShape).background(Brush.linearGradient(listOf(GREEN_A, GREEN_B))),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Filled.Call, "Nghe", tint = Color.White, modifier = Modifier.size(28.dp)) }

        // Nút kéo ở giữa
        val pulse = rememberInfiniteTransition(label = "hint")
        val nudge by pulse.animateFloat(
            -6f, 6f, infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse), label = "nudge"
        )
        Box(
            Modifier.align(Alignment.Center)
                .offset { IntOffset((shown + if (drag == 0f) nudge * density.density else 0f).roundToInt(), 0) }
                .size(58.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.92f))
                .draggable(
                    state = rememberDraggableState { d -> if (!done) drag = (drag + d).coerceIn(-max, max) },
                    orientation = Orientation.Horizontal,
                    onDragStopped = {
                        when {
                            drag > max * 0.75f -> { done = true; onAnswer() }
                            drag < -max * 0.75f -> { done = true; onDecline() }
                        }
                        drag = 0f
                    }
                ),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Filled.Call, null, tint = NAVY_TOP, modifier = Modifier.size(26.dp)) }
    }
}

// ───────────────────────── Đang gọi ─────────────────────────

@Composable
private fun OngoingScreen(call: Call, other: Call?) {
    val ctx = LocalContext.current
    val number = CallManager.number(call)
    val name by produceState(number.ifBlank { "Số ẩn" }, number) {
        value = withContext(Dispatchers.IO) { CallInfo.display(ctx, number) }
    }
    val st = CallManager.state(call)
    var keypad by remember { mutableStateOf(false) }
    var typed by remember { mutableStateOf("") }

    // Đồng hồ đếm giờ
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(call) { while (true) { now = System.currentTimeMillis(); delay(1000) } }
    val since = call.details?.connectTimeMillis ?: 0L
    val status = when (st) {
        Call.STATE_DIALING, Call.STATE_CONNECTING, Call.STATE_NEW -> "Đang gọi…"
        Call.STATE_HOLDING -> "Đang giữ máy"
        Call.STATE_DISCONNECTING, Call.STATE_DISCONNECTED -> "Đã kết thúc"
        Call.STATE_ACTIVE -> if (since > 0) formatDuration(now - since) else "00:00"
        else -> ""
    }

    // Tắt màn hình khi áp tai (chỉ khi đang nghe bằng loa trong)
    val earpiece = st == Call.STATE_ACTIVE && !CallManager.isSpeaker()
    DisposableEffect(earpiece) {
        val pm = ctx.getSystemService(PowerManager::class.java)
        val wl = if (earpiece && pm != null && pm.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK))
            pm.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, "danhbaso:proximity").apply { acquire(4 * 60 * 60 * 1000L) }
        else null
        onDispose { if (wl?.isHeld == true) wl.release() }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(20.dp))
        StatusChip(if (st == Call.STATE_ACTIVE) "Đang gọi" else status)
        if (other != null) {
            Spacer(Modifier.height(10.dp))
            OtherCallBar(other, canMerge = call.details?.can(Call.Details.CAPABILITY_MERGE_CONFERENCE) == true) {
                call.conference(other)
            }
        }
        Spacer(Modifier.height(if (other != null) 18.dp else 34.dp))
        CallAvatar(name, dia = 104, waves = false)
        Spacer(Modifier.height(20.dp))
        Text(name, color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(6.dp))
        Text(status, color = if (st == Call.STATE_ACTIVE) MINT else Color.White.copy(alpha = 0.8f), fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        if (name != number && number.isNotBlank()) {
            Text(prettyNumber(number), color = Color.White.copy(alpha = 0.6f), fontSize = 12.5.sp)
        }
        Spacer(Modifier.weight(1f))

        // Tấm kính chứa các nút
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(30.dp)).background(GLASS)
                .border(1.dp, GLASS_LINE, RoundedCornerShape(30.dp)).padding(horizontal = 10.dp, vertical = 16.dp),
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
                                        call.playDtmfTone(k[0])
                                        call.stopDtmfTone()
                                    },
                                contentAlignment = Alignment.Center
                            ) { Text(k, color = Color.White, fontSize = 24.sp) }
                        }
                    }
                }
                TextButton(onClick = { keypad = false }) { Text("Ẩn bàn phím", color = SKY) }
            } else {
                val held = st == Call.STATE_HOLDING
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    GlassButton(Icons.Filled.MicOff, "Tắt tiếng", CallManager.isMuted()) { CallManager.setMuted(!CallManager.isMuted()) }
                    GlassButton(Icons.Filled.Dialpad, "Bàn phím", false) { keypad = true }
                    GlassButton(Icons.Filled.VolumeUp, "Loa ngoài", CallManager.isSpeaker()) { CallManager.setSpeaker(!CallManager.isSpeaker()) }
                }
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    GlassButton(Icons.Filled.Pause, if (held) "Tiếp tục" else "Giữ máy", held) {
                        if (held) call.unhold() else call.hold()
                    }
                    GlassButton(Icons.Filled.PersonAdd, "Thêm người", false) {
                        // Mở bàn phím của app để gọi thêm (cuộc hiện tại tự giữ máy)
                        runCatching {
                            ctx.startActivity(Intent(Intent.ACTION_DIAL).setClass(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        }
                    }
                    GlassButton(Icons.Filled.Contacts, "Danh bạ", false) {
                        runCatching {
                            ctx.startActivity(Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(
                Modifier.width(170.dp).height(56.dp)
                    .shadow(16.dp, RoundedCornerShape(28.dp), ambientColor = RED_B, spotColor = RED_B)
                    .clip(RoundedCornerShape(28.dp)).background(Brush.linearGradient(listOf(RED_A, RED_B)))
                    .clickable { CallManager.hangup(call) },
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.CallEnd, null, tint = Color.White, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Text("Kết thúc", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(18.dp))
    }
}

/** Thanh cuộc gọi thứ 2 (đang giữ máy): Đổi qua lại, Gộp nếu nhà mạng cho phép. */
@Composable
private fun OtherCallBar(other: Call, canMerge: Boolean, onMerge: () -> Unit) {
    val ctx = LocalContext.current
    val num = CallManager.number(other)
    val name by produceState(num.ifBlank { "Số ẩn" }, num) {
        value = withContext(Dispatchers.IO) { CallInfo.display(ctx, num) }
    }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(GLASS)
            .border(1.dp, GLASS_LINE, RoundedCornerShape(18.dp)).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("Đang giữ máy", color = Color.White.copy(alpha = 0.65f), fontSize = 12.sp)
        }
        if (canMerge) SmallGlass(Icons.Filled.CallMerge, "Gộp", onMerge)
        Spacer(Modifier.width(6.dp))
        SmallGlass(Icons.Filled.SwapCalls, "Đổi") { CallManager.swap() }
    }
}

// ───────────────────────── Thành phần dùng chung ─────────────────────────

@Composable
private fun StatusChip(text: String) {
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
private fun CallAvatar(name: String, dia: Int, waves: Boolean) {
    val inf = rememberInfiniteTransition(label = "av")
    val w by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(1800, easing = LinearEasing)), label = "w")
    val rot by inf.animateFloat(0f, 360f, infiniteRepeatable(tween(6000, easing = LinearEasing)), label = "rot")
    val letter = name.trim().firstOrNull { it.isLetterOrDigit() }?.uppercase() ?: "?"
    val base = avatarColor(name)
    Box(Modifier.size((dia + 70).dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val r = dia.dp.toPx() / 2
            if (waves) {
                for (k in 0..1) {
                    val f = (w + k * 0.5f) % 1f
                    drawCircle(SKY.copy(alpha = 0.45f * (1f - f)), r + 6.dp.toPx() + f * 30.dp.toPx(), style = Stroke(1.5.dp.toPx()))
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
        ) { Text(letter, color = Color.White, fontSize = (dia * 0.38f).sp, fontWeight = FontWeight.Bold) }
    }
}

/** Nút vuông bo góc dạng kính mờ; đang bật thì phát sáng xanh. */
@Composable
private fun GlassButton(icon: ImageVector, label: String, on: Boolean, onClick: () -> Unit) {
    Column(Modifier.width(96.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(60.dp)
                .then(if (on) Modifier.shadow(14.dp, RoundedCornerShape(20.dp), ambientColor = SKY, spotColor = SKY) else Modifier)
                .clip(RoundedCornerShape(20.dp))
                .background(if (on) Brush.linearGradient(listOf(SKY, Color(0xFF2F86FF))) else Brush.linearGradient(listOf(GLASS, GLASS)))
                .border(1.dp, if (on) Color.Transparent else GLASS_LINE, RoundedCornerShape(20.dp))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) { Icon(icon, label, tint = Color.White, modifier = Modifier.size(26.dp)) }
        Spacer(Modifier.height(6.dp))
        Text(label, color = Color.White.copy(alpha = 0.9f), fontSize = 12.5.sp)
    }
}

@Composable
private fun GlassChip(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(16.dp)).background(GLASS).border(1.dp, GLASS_LINE, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = Color.White, fontSize = 13.5.sp)
    }
}

@Composable
private fun SmallGlass(icon: ImageVector, label: String, onClick: () -> Unit) {
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

private fun formatDuration(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%02d:%02d".format(m, sec)
}

/** 0917600108 → 0917 600 108 */
private fun prettyNumber(n: String): String = try {
    com.quoctuan.danhbaso.util.PhoneUtil.pretty(n)
} catch (e: Exception) {
    n
}
