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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMerge
import androidx.compose.material.icons.filled.SwapCalls
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quoctuan.danhbaso.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext


// Màu riêng của màn hình gọi (nền tối, không đổi theo bộ màu app để luôn dễ nhìn)

/** Màn hình cuộc gọi đến / đang gọi của Danh bạ số. Không qua cổng kiểm tra bảo mật để không lỡ cuộc gọi. */
class CallActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showOnLockScreen()
        enableEdgeToEdge()
        handle(intent)
        // Theo ☰ → Cỡ chữ của app
        val factor = com.quoctuan.danhbaso.data.FONT_FACTORS
            .getOrElse(com.quoctuan.danhbaso.data.Prefs(this).fontSize) { 1f }
        setContent {
            val d = LocalDensity.current
            androidx.compose.runtime.CompositionLocalProvider(
                LocalDensity provides androidx.compose.ui.unit.Density(d.density, d.fontScale * factor)
            ) { CallRoot(onClose = { finish() }) }
        }
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
    // Đọc "tick" để màn hình vẽ lại mỗi khi trạng thái cuộc gọi đổi
    remember(CallManager.tick) { CallManager.tick }
    val current = CallManager.current()
    val other = if (GroupCall.active) null else CallManager.calls.firstOrNull {
        it != current && it.parent == null && CallManager.state(it) != Call.STATE_DISCONNECTED
    }

    // Hết cuộc gọi: hiện "Đã kết thúc" chốc lát rồi đóng
    LaunchedEffect(current == null) {
        if (current == null) {
            delay(900)
            onClose()
        }
    }

    Box(Modifier.fillMaxSize().callBackground().safeDrawingPadding()) {
        // 0 = đã kết thúc, 1 = đang đổ chuông, 2 = đang gọi. Đổi giữa các màn có hiệu ứng mượt.
        val phase = when {
            current == null -> 0
            CallManager.state(current) == Call.STATE_RINGING -> 1
            else -> 2
        }
        AnimatedContent(
            targetState = phase,
            transitionSpec = {
                (fadeIn(tween(400)) + scaleIn(tween(400), initialScale = 1.06f) +
                    slideInVertically(tween(400)) { it / 10 }) togetherWith
                    (fadeOut(tween(250)) + scaleOut(tween(300), targetScale = 0.94f))
            },
            label = "phase"
        ) { ph ->
            val c = CallManager.current()
            when {
                ph == 0 || c == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Cuộc gọi đã kết thúc", color = Color.White, fontSize = 20.sp)
                }
                ph == 1 && CallManager.state(c) == Call.STATE_RINGING -> IncomingScreen(c, other)
                GroupCall.active -> GroupCallScreen(c)
                else -> OngoingScreen(c, other)
            }
        }
    }
}

// ───────────────────────── Cuộc gọi đến ─────────────────────────

@Composable
private fun IncomingScreen(call: Call, other: Call?) {
    val ctx = LocalContext.current
    val number = CallManager.number(call)
    // Tên trong danh bạ (null = số chưa lưu)
    val saved by produceState<String?>(null, number) {
        value = withContext(Dispatchers.IO) { CallInfo.name(ctx, number) }
    }
    val sim = remember(call) { CallInfo.simLabel(ctx, call) }
    IncomingContent(
        saved = saved, number = number, sim = sim, waiting = other != null,
        onAnswer = { CallManager.answer(call) },
        onDecline = { CallManager.decline(call) },
        onSpeaker = { CallManager.answer(call, speaker = true) },
        onSmsReply = { CallManager.declineWithText(call, it) },
    )
}

// ───────────────────────── Đang gọi ─────────────────────────

@Composable
private fun OngoingScreen(call: Call, other: Call?) {
    val ctx = LocalContext.current
    val number = CallManager.number(call)
    val saved by produceState<String?>(null, number) {
        value = withContext(Dispatchers.IO) { CallInfo.name(ctx, number) }
    }
    val st = CallManager.state(call)

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

    OngoingContent(
        saved = saved, number = number, status = status,
        active = st == Call.STATE_ACTIVE, held = st == Call.STATE_HOLDING,
        muted = CallManager.isMuted(), speaker = CallManager.isSpeaker(),
        otherBar = if (other == null) null else {
            {
                OtherCallBar(other, canMerge = call.details?.can(Call.Details.CAPABILITY_MERGE_CONFERENCE) == true) {
                    call.conference(other)
                }
            }
        },
        onMute = { CallManager.setMuted(!CallManager.isMuted()) },
        onSpeaker = { CallManager.setSpeaker(!CallManager.isSpeaker()) },
        onHold = { if (st == Call.STATE_HOLDING) call.unhold() else call.hold() },
        onDtmf = { k -> call.playDtmfTone(k); call.stopDtmfTone() },
        onAddPerson = {
            // Mở bàn phím của app để gọi thêm (cuộc hiện tại tự giữ máy)
            runCatching {
                ctx.startActivity(Intent(Intent.ACTION_DIAL).setClass(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        },
        onContacts = {
            runCatching { ctx.startActivity(Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        },
        onHangup = { CallManager.hangup(call) },
    )
}

// ───────────────────────── Gọi nhóm ─────────────────────────

/** Màn gọi nhóm: danh sách người + trạng thái, app tự gọi lần lượt và tự gộp. */
@Composable
private fun GroupCallScreen(call: Call) {
    val ctx = LocalContext.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(1000) } }
    val since = call.details?.connectTimeMillis ?: 0L
    GroupCallContent(
        title = GroupCall.title,
        members = GroupCall.members.map { GroupRow(it.name, it.status) },
        timer = if (since > 0 && CallManager.state(call) == Call.STATE_ACTIVE) formatDuration(now - since) else null,
        muted = CallManager.isMuted(), speaker = CallManager.isSpeaker(),
        onMute = { CallManager.setMuted(!CallManager.isMuted()) },
        onSpeaker = { CallManager.setSpeaker(!CallManager.isSpeaker()) },
        onAddPerson = {
            runCatching {
                ctx.startActivity(Intent(Intent.ACTION_DIAL).setClass(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        },
        onRetry = { i -> GroupCall.members.getOrNull(i)?.let { GroupCall.retry(it) } },
        onKick = { i -> GroupCall.members.getOrNull(i)?.let { GroupCall.kick(it) } },
        onEnd = { GroupCall.endAll() },
    )
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
        if (canMerge) SmallGlass(Icons.AutoMirrored.Filled.CallMerge, "Gộp", onMerge)
        Spacer(Modifier.width(6.dp))
        SmallGlass(Icons.Filled.SwapCalls, "Đổi") { CallManager.swap() }
    }
}

