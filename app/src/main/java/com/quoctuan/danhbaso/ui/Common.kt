package com.quoctuan.danhbaso.ui

import android.provider.CallLog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quoctuan.danhbaso.data.CallEntry
import com.quoctuan.danhbaso.util.TextUtil
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Bảng màu theo bản thiết kế. Tự đổi theo chế độ Sáng / Tối của điện thoại. */
object C {
    var Bg = Color(0xFF14171F)
        private set
    var Card = Color(0xFF1F232D)
        private set
    var Card2 = Color(0xFF262B37)
        private set
    var Search = Color(0xFF232835)
        private set
    var Nav = Color(0xFF1A1E27)
        private set
    var Text = Color(0xFFE6E8EE)
        private set
    var Sub = Color(0xFF9AA1B0)
        private set
    var Sub2 = Color(0xFFC4C9D4)
        private set
    var Blue = Color(0xFF8AB4F8)
        private set
    var OnBlue = Color(0xFF0B1A33)
        private set
    var BlueBg = Color(0xFF2C3A55)
        private set
    var Green = Color(0xFF81C995)
        private set
    var OnGreen = Color(0xFF0E2A18)
        private set
    var GreenBg = Color(0xFF1D3326)
        private set
    var GreenDark = Color(0xFF1E5631)
        private set
    var Red = Color(0xFFF28B82)
        private set
    var Orange = Color(0xFFFCAD70)
        private set
    var OnOrange = Color(0xFF3A2200)
        private set
    var OrangeBg = Color(0xFF3A2E1A)
        private set
    var OrangeText = Color(0xFFF3D9B8)
        private set
    var Grey = Color(0xFFAAB0BD)
        private set
    var Border = Color(0xFF444B5C)
        private set
    var Line = Color(0xFF2A2F3B)
        private set
    var TickOff = Color(0xFF5A6172)
        private set
    var Chip = Color(0xFF3A4257)
        private set
    var KeyBorder = Color(0xFF333B4D)
        private set
    var NoAvatar = Color(0xFF3A3F4B)
        private set
    val Zalo = Color(0xFF0068FF)
    var Yellow = Color(0xFFFDD663)
        private set
    var dark = true
        private set

    private var initialized = false

    /** Nền phía trên (sáng nhẹ) + dải màu điểm nhấn cyan → xanh dương. */
    var BgTop = Color(0xFF173A68)
        private set
    var AccentA = Color(0xFF3FD0FF)
        private set
    var AccentB = Color(0xFF2F86FF)
        private set
    val accent: androidx.compose.ui.graphics.Brush
        get() = androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(AccentA, AccentB))
    val background: androidx.compose.ui.graphics.Brush
        get() = androidx.compose.ui.graphics.Brush.verticalGradient(listOf(BgTop, Bg, Bg), endY = 1600f)

    fun apply(isDark: Boolean) {
        if (initialized && isDark == dark) return
        initialized = true
        dark = isDark
        if (isDark) {
            // Tông "Navy biển"
            BgTop = Color(0xFF173A68); Bg = Color(0xFF0B2342); Card = Color(0xFF12304F); Card2 = Color(0xFF143458)
            Search = Color(0xFF173A63); Nav = Color(0xFF0A1F3B); Text = Color(0xFFEAF2FF); Sub = Color(0xFF94A9CC)
            Sub2 = Color(0xFFBFD0EA); Blue = Color(0xFF5CC2FF); OnBlue = Color(0xFF06203D); BlueBg = Color(0xFF1B4373)
            AccentA = Color(0xFF3FD0FF); AccentB = Color(0xFF2F86FF)
            Green = Color(0xFF5EE0A0); OnGreen = Color(0xFF05301C); GreenBg = Color(0xFF113E3E); GreenDark = Color(0xFF15584A)
            Red = Color(0xFFFF7A85); Orange = Color(0xFFFFB35C); OnOrange = Color(0xFF3A2200)
            OrangeBg = Color(0xFF3A3528); OrangeText = Color(0xFFFFE2BC); Grey = Color(0xFF9AA9C8)
            Border = Color(0xFF2A4C80); Line = Color(0xFF244470); TickOff = Color(0xFF5A7099)
            Chip = Color(0xFF1B4373); KeyBorder = Color(0xFF2A4C80); NoAvatar = Color(0xFF223F66); Yellow = Color(0xFFFFD166)
        } else {
            BgTop = Color(0xFFE6F0FC); Bg = Color(0xFFF5F8FD); Card = Color(0xFFFFFFFF); Card2 = Color(0xFFFFFFFF)
            Search = Color(0xFFE6EDF7); Nav = Color(0xFFFFFFFF); Text = Color(0xFF13233D); Sub = Color(0xFF5B6B85)
            Sub2 = Color(0xFF34445E); Blue = Color(0xFF1A6FD6); OnBlue = Color(0xFFFFFFFF); BlueBg = Color(0xFFDCEBFC)
            AccentA = Color(0xFF1FB6E8); AccentB = Color(0xFF1A6FD6)
            Green = Color(0xFF1E8E4E); OnGreen = Color(0xFFFFFFFF); GreenBg = Color(0xFFE2F4E8); GreenDark = Color(0xFFD4EEDD)
            Red = Color(0xFFD13B2F); Orange = Color(0xFFC25E00); OnOrange = Color(0xFFFFFFFF)
            OrangeBg = Color(0xFFFFF0DE); OrangeText = Color(0xFF6B3A00); Grey = Color(0xFF6B7890)
            Border = Color(0xFFC6D3E6); Line = Color(0xFFDCE5F2); TickOff = Color(0xFF9AA8BF)
            Chip = Color(0xFFDCEBFC); KeyBorder = Color(0xFFBFCDE3); NoAvatar = Color(0xFFD2DCEB); Yellow = Color(0xFFD99A00)
        }
    }
}

private val avatarPalette = listOf(
    Color(0xFF1E7FA8), Color(0xFF2F6FB3), Color(0xFF2E7D4F), Color(0xFFB0457A),
    Color(0xFF8A5A2B), Color(0xFF8F2420), Color(0xFF9C1F5E), Color(0xFF1E5631),
)

fun avatarColor(name: String): Color = avatarPalette[(name.hashCode() and 0x7fffffff) % avatarPalette.size]

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val dark = androidx.compose.foundation.isSystemInDarkTheme()
    C.apply(dark)
    val scheme = if (dark) darkColorScheme(
        primary = C.Blue, onPrimary = C.OnBlue, background = C.Bg, onBackground = C.Text,
        surface = C.Card, onSurface = C.Text, surfaceVariant = C.Card2, onSurfaceVariant = C.Sub2,
        secondaryContainer = C.Chip, onSecondaryContainer = C.Text, surfaceContainer = C.Nav,
    ) else androidx.compose.material3.lightColorScheme(
        primary = C.Blue, onPrimary = C.OnBlue, background = C.Bg, onBackground = C.Text,
        surface = C.Card, onSurface = C.Text, surfaceVariant = C.Search, onSurfaceVariant = C.Sub2,
        secondaryContainer = C.Chip, onSecondaryContainer = C.Text, surfaceContainer = C.Nav,
    )
    MaterialTheme(colorScheme = scheme, content = content)
}

/** Trạng thái cuộc gọi và màu tương ứng (đã thống nhất). */
enum class CallStatus(val label: String) {
    ANSWERED("Đã bắt máy"),
    MISSED("Chưa bắt máy"),
    REJECTED("Đã từ chối"),
    OUTGOING("Gọi đi · Đã nghe máy"),
    NO_ANSWER("Gọi đi · Không nghe máy"),
    BLOCKED("Đã chặn"),
    VOICEMAIL("Thư thoại");

    /** Màu lấy theo chế độ Sáng/Tối hiện tại. */
    val color: Color
        get() = when (this) {
            ANSWERED -> C.Green
            MISSED -> C.Red
            REJECTED -> C.Orange
            OUTGOING -> C.Blue
            else -> C.Grey
        }
}

fun statusOf(e: CallEntry): CallStatus = when (e.type) {
    CallLog.Calls.INCOMING_TYPE, CallLog.Calls.ANSWERED_EXTERNALLY_TYPE -> CallStatus.ANSWERED
    CallLog.Calls.MISSED_TYPE -> CallStatus.MISSED
    CallLog.Calls.REJECTED_TYPE -> CallStatus.REJECTED
    CallLog.Calls.BLOCKED_TYPE -> CallStatus.BLOCKED
    CallLog.Calls.VOICEMAIL_TYPE -> CallStatus.VOICEMAIL
    CallLog.Calls.OUTGOING_TYPE -> if (e.duration > 0) CallStatus.OUTGOING else CallStatus.NO_ANSWER
    else -> CallStatus.ANSWERED
}

fun isOutgoing(e: CallEntry) = e.type == CallLog.Calls.OUTGOING_TYPE

fun formatDuration(sec: Long): String {
    if (sec <= 0) return ""
    val m = sec / 60
    val s = sec % 60
    return if (m > 0) "$m phút ${s.toString().padStart(2, '0')} giây" else "$s giây"
}

private fun todayStart(): Long = Calendar.getInstance().apply {
    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
}.timeInMillis

/** 0 = hôm nay, 1 = hôm qua, 2 = cũ hơn. */
fun dayBucket(time: Long): Int {
    val start = todayStart()
    return when {
        time >= start -> 0
        time >= start - 86_400_000L -> 1
        else -> 2
    }
}

private val hmFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
private val dmFormat = SimpleDateFormat("HH:mm d 'thg' M", Locale.getDefault())

/** Định dạng giờ (gọi trên luồng giao diện). */
fun formatTime(time: Long): String =
    if (time >= todayStart()) hmFormat.format(Date(time)) else dmFormat.format(Date(time))

/** Vòng tròn chữ cái đầu (hoặc biểu tượng người nếu chưa lưu tên). */
@Composable
fun Avatar(name: String?, size: Dp = 44.dp, color: Color? = null, letter: String? = null) {
    val bg = if (name == null) C.NoAvatar else (color ?: avatarColor(name))
    Box(
        Modifier.size(size).clip(CircleShape).background(bg),
        contentAlignment = Alignment.Center
    ) {
        if (name == null) {
            Icon(Icons.Filled.Person, null, tint = C.Sub2, modifier = Modifier.size(size * 0.55f))
        } else {
            val l = letter ?: androidx.compose.runtime.remember(name) { TextUtil.sectionLetter(name) }
            Text(
                l.takeIf { it != "#" } ?: name.take(1),
                color = Color.White, fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.42f).sp
            )
        }
    }
}

@Composable
fun TopBar(title: String, onBack: () -> Unit, actions: @Composable () -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().padding(start = 4.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Quay lại", tint = C.Text) }
        Text(title, color = C.Text, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        actions()
    }
}

/** Nút tròn có chữ bên dưới (Gọi, Nhắn tin, Zalo...). */
@Composable
fun RoundAction(
    label: String, bg: Color, fg: Color, onClick: () -> Unit,
    icon: ImageVector? = null, text: String? = null,
) {
    Column(
        Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(Modifier.size(56.dp).clip(CircleShape).background(bg), contentAlignment = Alignment.Center) {
            if (icon != null) Icon(icon, null, tint = fg)
            if (text != null) Text(text, color = fg, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
        Text(label, color = C.Text, fontSize = 13.sp)
    }
}

/** Ô chọn có viền xanh + dấu tích xanh (Lưu vào SIM / Gmail). */
@Composable
fun SelectCard(
    selected: Boolean, enabled: Boolean, title: String, subtitle: String,
    icon: ImageVector, onClick: () -> Unit, subtitleClick: (() -> Unit)? = null,
) {
    val border = if (selected) C.Green else C.Line
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(if (selected) C.GreenBg else C.Card)
            .border(2.dp, border, RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(C.Card2), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = C.Sub2)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = if (enabled) C.Text else C.Sub, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(
                subtitle, color = if (subtitleClick != null) C.Blue else C.Sub, fontSize = 12.sp,
                modifier = if (subtitleClick != null) Modifier.clickable(onClick = subtitleClick) else Modifier
            )
        }
        Box(
            Modifier.size(30.dp).clip(CircleShape)
                .background(if (selected) C.Green else Color.Transparent)
                .border(2.dp, if (selected) C.Green else C.TickOff, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (selected) Icon(Icons.Filled.Check, null, tint = C.OnGreen, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(text, color = C.Blue, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 8.dp, top = 12.dp, bottom = 6.dp))
}

@Composable
fun CardBox(modifier: Modifier = Modifier, bg: Color = C.Card, content: @Composable () -> Unit) {
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(bg).padding(16.dp)) { content() }
}

@Composable
fun Gap(h: Int) = Spacer(Modifier.height(h.dp))

/** Viền phát sáng mảnh (cyan → xanh dương). */
fun Modifier.glowBorder(shape: androidx.compose.ui.graphics.Shape, width: Dp = 1.dp): Modifier {
    val a = if (C.dark) 0.65f else 0.45f
    return this.border(
        width,
        androidx.compose.ui.graphics.Brush.linearGradient(listOf(C.AccentA.copy(alpha = a), C.AccentB.copy(alpha = a * 0.6f))),
        shape
    )
}

/** Quầng sáng tỏa ra xung quanh (dùng cho nút quan trọng). */
fun Modifier.glow(
    shape: androidx.compose.ui.graphics.Shape,
    elevation: Dp = 12.dp,
    color: Color = C.AccentA,
): Modifier = this.shadow(elevation, shape, clip = false, ambientColor = color, spotColor = color)
