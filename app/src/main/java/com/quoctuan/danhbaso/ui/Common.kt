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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
    var Bg by mutableStateOf(Color(0xFF14171F))
        private set
    var Card by mutableStateOf(Color(0xFF1F232D))
        private set
    var Card2 by mutableStateOf(Color(0xFF262B37))
        private set
    var Search by mutableStateOf(Color(0xFF232835))
        private set
    var Nav by mutableStateOf(Color(0xFF1A1E27))
        private set
    var Text by mutableStateOf(Color(0xFFE6E8EE))
        private set
    var Sub by mutableStateOf(Color(0xFF9AA1B0))
        private set
    var Sub2 by mutableStateOf(Color(0xFFC4C9D4))
        private set
    var Blue by mutableStateOf(Color(0xFF8AB4F8))
        private set
    var OnBlue by mutableStateOf(Color(0xFF0B1A33))
        private set
    var BlueBg by mutableStateOf(Color(0xFF2C3A55))
        private set
    var Green by mutableStateOf(Color(0xFF81C995))
        private set
    var OnGreen by mutableStateOf(Color(0xFF0E2A18))
        private set
    var GreenBg by mutableStateOf(Color(0xFF1D3326))
        private set
    var GreenDark by mutableStateOf(Color(0xFF1E5631))
        private set
    var Red by mutableStateOf(Color(0xFFF28B82))
        private set
    var Orange by mutableStateOf(Color(0xFFFCAD70))
        private set
    var OnOrange by mutableStateOf(Color(0xFF3A2200))
        private set
    var OrangeBg by mutableStateOf(Color(0xFF3A2E1A))
        private set
    var OrangeText by mutableStateOf(Color(0xFFF3D9B8))
        private set
    var Grey by mutableStateOf(Color(0xFFAAB0BD))
        private set
    var Border by mutableStateOf(Color(0xFF444B5C))
        private set
    var Line by mutableStateOf(Color(0xFF2A2F3B))
        private set
    var TickOff by mutableStateOf(Color(0xFF5A6172))
        private set
    var Chip by mutableStateOf(Color(0xFF3A4257))
        private set
    var KeyBorder by mutableStateOf(Color(0xFF333B4D))
        private set
    var NoAvatar by mutableStateOf(Color(0xFF3A3F4B))
        private set
    val Zalo = Color(0xFF0068FF)
    var Yellow by mutableStateOf(Color(0xFFFDD663))
        private set
    var dark = true
        private set
    /** Bộ màu đang dùng: 0 Navy biển, 1 Xanh lá rừng, 2 Xanh ngọc, 3 Than chì. */
    var theme = 0
        private set
    /** Màu xanh dương cố định cho trạng thái "Gọi đi" (không đổi theo bộ màu). */
    var CallBlue by mutableStateOf(Color(0xFF5CC2FF))
        private set

    private var initialized = false

    /** Nền phía trên (sáng nhẹ) + dải màu điểm nhấn cyan → xanh dương. */
    var BgTop by mutableStateOf(Color(0xFF173A68))
        private set
    var AccentA by mutableStateOf(Color(0xFF3FD0FF))
        private set
    var AccentB by mutableStateOf(Color(0xFF2F86FF))
        private set
    val accent: androidx.compose.ui.graphics.Brush
        get() = androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(AccentA, AccentB))
    val background: androidx.compose.ui.graphics.Brush
        get() = androidx.compose.ui.graphics.Brush.verticalGradient(listOf(BgTop, Bg, Bg), endY = 1600f)

    fun apply(isDark: Boolean, themeIndex: Int = theme) {
        if (initialized && isDark == dark && themeIndex == theme) return
        initialized = true
        dark = isDark
        theme = themeIndex
        CallBlue = if (isDark) Color(0xFF5CC2FF) else Color(0xFF1A6FD6)
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
        if (themeIndex != 0) overlay(isDark, themeIndex)
    }

    /** Ghi đè màu nền / viền / điểm nhấn theo bộ màu đã chọn. Màu trạng thái cuộc gọi giữ nguyên. */
    private fun overlay(isDark: Boolean, t: Int) {
        // thứ tự: BgTop, Bg, Card, Card2, Search, Nav, Text, Sub, Sub2, Blue, OnBlue, BlueBg,
        //         AccentA, AccentB, Border, Line, TickOff, Chip, KeyBorder, NoAvatar
        val p: LongArray = when (t) {
            1 -> if (isDark) longArrayOf(0xFF14503D, 0xFF0A2A21, 0xFF0F3A2D, 0xFF113F31, 0xFF144838, 0xFF08221A, 0xFFEAFBF3, 0xFF8FBFAA, 0xFFBFE3D3, 0xFF6EF2B4, 0xFF05301C, 0xFF175A44, 0xFF6EF2B4, 0xFF1FAF78, 0xFF2A6B53, 0xFF1E5A45, 0xFF5A8F7A, 0xFF175A44, 0xFF2A6B53, 0xFF1D5040)
                 else longArrayOf(0xFFE3F5EC, 0xFFF4FAF7, 0xFFFFFFFF, 0xFFFFFFFF, 0xFFE3F0EA, 0xFFFFFFFF, 0xFF10291F, 0xFF557366, 0xFF2F4A3F, 0xFF15885A, 0xFFFFFFFF, 0xFFD8F0E4, 0xFF2CC98A, 0xFF15885A, 0xFFBFDCCD, 0xFFD9EBE2, 0xFF98B8A9, 0xFFD8F0E4, 0xFFB5D6C6, 0xFFCFE5DA)
            2 -> if (isDark) longArrayOf(0xFF11505A, 0xFF07272D, 0xFF0D3940, 0xFF0F3F47, 0xFF124650, 0xFF061F24, 0xFFE8FAFB, 0xFF8DBCC2, 0xFFC0E2E6, 0xFF5CF0EA, 0xFF042A2E, 0xFF155B64, 0xFF5CF0EA, 0xFF13A7BC, 0xFF22707A, 0xFF1D5660, 0xFF5A8E95, 0xFF155B64, 0xFF22707A, 0xFF1A4C54)
                 else longArrayOf(0xFFE0F5F6, 0xFFF3FAFB, 0xFFFFFFFF, 0xFFFFFFFF, 0xFFE0EFF1, 0xFFFFFFFF, 0xFF0E2A2E, 0xFF52737A, 0xFF2D4A50, 0xFF0E8A9C, 0xFFFFFFFF, 0xFFD5F0F3, 0xFF1EC3D0, 0xFF0E8A9C, 0xFFBCDCE0, 0xFFD6EBEE, 0xFF94B8BD, 0xFFD5F0F3, 0xFFB2D6DB, 0xFFCCE4E7)
            else -> if (isDark) longArrayOf(0xFF2A303B, 0xFF14171D, 0xFF1D222B, 0xFF222833, 0xFF262C36, 0xFF101318, 0xFFECEFF4, 0xFF9AA3B2, 0xFFC7CDD8, 0xFFB9CCEB, 0xFF141A26, 0xFF2E3748, 0xFFC9D6EA, 0xFF6F86AD, 0xFF3C4556, 0xFF323A47, 0xFF5F6878, 0xFF2E3748, 0xFF46516A, 0xFF343B48)
                 else longArrayOf(0xFFECEFF4, 0xFFF6F7F9, 0xFFFFFFFF, 0xFFFFFFFF, 0xFFE7EAEF, 0xFFFFFFFF, 0xFF1A1F29, 0xFF5E6674, 0xFF39404D, 0xFF4A6491, 0xFFFFFFFF, 0xFFE1E7F1, 0xFF8FA6CC, 0xFF4A6491, 0xFFC9CFDA, 0xFFDFE3EA, 0xFFA0A8B6, 0xFFE1E7F1, 0xFFC3CAD6, 0xFFD5DAE3)
        }
        fun c(i: Int) = Color(p[i])
        BgTop = c(0); Bg = c(1); Card = c(2); Card2 = c(3); Search = c(4); Nav = c(5)
        Text = c(6); Sub = c(7); Sub2 = c(8); Blue = c(9); OnBlue = c(10); BlueBg = c(11)
        AccentA = c(12); AccentB = c(13); Border = c(14); Line = c(15); TickOff = c(16)
        Chip = c(17); KeyBorder = c(18); NoAvatar = c(19)
    }
}

/** Tên các bộ màu (hiện trong menu ☰ → Màu giao diện). */
val THEME_NAMES = listOf("Navy biển", "Xanh lá rừng", "Xanh ngọc", "Than chì")

/** Màu mẫu (điểm nhấn, nền) để vẽ vòng tròn xem trước. */
val THEME_SWATCH = listOf(
    Color(0xFF3FD0FF) to Color(0xFF0B2342),
    Color(0xFF6EF2B4) to Color(0xFF0A2A21),
    Color(0xFF5CF0EA) to Color(0xFF07272D),
    Color(0xFFC9D6EA) to Color(0xFF14171D),
)

private val avatarPalette = listOf(
    Color(0xFF1E7FA8), Color(0xFF2F6FB3), Color(0xFF2E7D4F), Color(0xFFB0457A),
    Color(0xFF8A5A2B), Color(0xFF8F2420), Color(0xFF9C1F5E), Color(0xFF1E5631),
)

fun avatarColor(name: String): Color = avatarPalette[(name.hashCode() and 0x7fffffff) % avatarPalette.size]

@Composable
fun AppTheme(theme: Int = 0, content: @Composable () -> Unit) {
    val dark = androidx.compose.foundation.isSystemInDarkTheme()
    C.apply(dark, theme)
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
    ANSWERED("Gọi đến · Đã nghe máy"),
    MISSED("Gọi đến · Chưa nghe máy"),
    REJECTED("Gọi đến · Đã từ chối"),
    OUTGOING("Gọi đi · Đã nghe máy"),
    NO_ANSWER("Gọi đi · Không nghe máy"),
    BLOCKED("Gọi đến · Đã chặn"),
    VOICEMAIL("Gọi đến · Thư thoại");

    /** Màu lấy theo chế độ Sáng/Tối hiện tại. */
    val color: Color
        get() = when (this) {
            ANSWERED -> C.Green
            MISSED -> C.Red
            REJECTED -> C.Orange
            OUTGOING -> C.CallBlue
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
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại", tint = C.Text) }
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
