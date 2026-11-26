package com.quoctuan.danhbaso.ui

import android.content.Intent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.quoctuan.danhbaso.R
import com.quoctuan.danhbaso.data.AppData
import com.quoctuan.danhbaso.data.SecItem
import com.quoctuan.danhbaso.data.SecLevel
import com.quoctuan.danhbaso.data.SecReport
import com.quoctuan.danhbaso.data.SecurityCheck
import com.quoctuan.danhbaso.util.Actions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val SAFE_GREEN = Color(0xFF3DDC84)

/**
 * Cổng kiểm tra bảo mật khi mở app.
 * 🟢 an toàn → tự vào app · 🟡 có rủi ro → cảnh báo, vẫn cho vào · 🔴 app bị sửa → chặn, chỉ gọi khẩn cấp.
 */
@Composable
fun SecurityGate(data: AppData, onPass: () -> Unit) {
    val ctxApp = LocalContext.current.applicationContext
    var report by remember { mutableStateOf<SecReport?>(null) }
    var safeShown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        val started = System.currentTimeMillis()
        val r = withContext(Dispatchers.IO) { SecurityCheck.run(ctxApp) }
        val wait = 600 - (System.currentTimeMillis() - started)
        if (wait > 0) delay(wait)
        report = r
        val ignored = data.prefs.ignoredSecurity
        if (r.danger.isEmpty() && r.warnings.none { it.id !in ignored }) {
            safeShown = true
            delay(900)
            onPass()
        }
    }
    val r = report
    when {
        r == null -> CheckingScreen()
        r.danger.isNotEmpty() -> BlockedScreen(r.danger)
        safeShown -> SafeScreen(r)
        else -> WarningScreen(
            items = r.warnings.filter { it.id !in data.prefs.ignoredSecurity },
            onContinue = { ignoreIds ->
                if (ignoreIds.isNotEmpty()) data.prefs.ignoredSecurity = data.prefs.ignoredSecurity + ignoreIds
                onPass()
            }
        )
    }
}

/** Logo app (nền + hình), bo góc. */
@Composable
private fun AppLogo(size: Dp) {
    Box(Modifier.size(size).clip(RoundedCornerShape(size * 0.26f))) {
        val m = Modifier.requiredSize(size * 1.5f)
        Image(painterResource(R.drawable.ic_launcher_background), null, m.align(Alignment.Center))
        Image(painterResource(R.drawable.ic_launcher_foreground), null, m.align(Alignment.Center))
    }
}

/** Logo + vòng sáng xoay (màu [a] → [b]). */
@Composable
private fun GlowLogo(a: Color, b: Color, spin: Boolean, badge: (@Composable () -> Unit)? = null) {
    val t = rememberInfiniteTransition(label = "ring")
    val angle by t.animateFloat(0f, 360f, infiniteRepeatable(tween(2400, easing = LinearEasing)), label = "a")
    val pulse by t.animateFloat(0.55f, 1f, infiniteRepeatable(tween(1100), RepeatMode.Reverse), label = "p")
    Box(Modifier.size(210.dp), contentAlignment = Alignment.Center) {
        // 2 vòng lan tỏa mờ
        Box(Modifier.size(206.dp).clip(CircleShape).border(2.dp, a.copy(alpha = 0.18f * pulse), CircleShape))
        Box(Modifier.size(182.dp).clip(CircleShape).border(2.dp, a.copy(alpha = 0.35f * pulse), CircleShape))
        // Vòng sáng chuyển màu (xoay khi đang kiểm tra)
        Box(
            Modifier.size(162.dp).glow(CircleShape, 22.dp, a).rotate(if (spin) angle else 0f).clip(CircleShape)
                .background(Brush.sweepGradient(listOf(a, b, a.copy(alpha = 0.4f), a)))
        )
        Box(Modifier.size(148.dp).clip(CircleShape).background(Brush.radialGradient(listOf(C.BgTop, C.Bg))))
        AppLogo(96.dp)
        if (badge != null) Box(Modifier.size(162.dp), contentAlignment = Alignment.BottomEnd) { badge() }
    }
}

@Composable
private fun CheckingScreen() {
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        GlowLogo(C.AccentA, C.AccentB, spin = true)
        Spacer(Modifier.height(24.dp))
        Text("Danh bạ số", color = C.Text, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Đang kiểm tra bảo mật…", color = C.Sub2, fontSize = 16.sp)
    }
}

@Composable
private fun SafeScreen(r: SecReport) {
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        GlowLogo(SAFE_GREEN, C.AccentA, spin = false) {
            Box(
                Modifier.offset(6.dp, 6.dp).size(54.dp).glow(CircleShape, 14.dp, SAFE_GREEN).clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Color(0xFF6EF2B4), Color(0xFF1FAF78))))
                    .border(4.dp, C.Bg, CircleShape),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(30.dp)) }
        }
        Spacer(Modifier.height(26.dp))
        Text("Thiết bị an toàn", color = SAFE_GREEN, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(6.dp))
        Text("Đang vào Danh bạ số…", color = C.Sub2, fontSize = 15.sp)
        Spacer(Modifier.height(18.dp))
        val okItems = r.items.filter { it.level == SecLevel.OK }.map { it.title }
        // Các mục đạt, xếp 2 cột
        okItems.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 3.dp)) {
                row.forEach { t ->
                    Text(
                        "✓ $t", color = SAFE_GREEN, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(C.GreenBg).padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun WarningScreen(items: List<SecItem>, onContinue: (Set<String>) -> Unit) {
    val ctx = LocalContext.current
    var dontAsk by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Spacer(Modifier.height(24.dp))
        Text("⚠️ Có ${items.size} điểm cần lưu ý", color = C.Orange, fontSize = 23.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text("Bạn vẫn dùng app được, nhưng nên khắc phục để danh bạ an toàn hơn.", color = C.Sub2, fontSize = 15.sp)
        Spacer(Modifier.height(14.dp))
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(C.Orange.copy(alpha = 0.12f))
                .border(1.dp, C.Orange, RoundedCornerShape(18.dp)).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            for (i in items) {
                Column {
                    Text(i.title, color = C.Text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    if (i.detail.isNotEmpty()) Text(i.detail, color = C.Sub2, fontSize = 13.sp)
                    i.settingsAction?.let { act ->
                        Text(
                            "Mở Cài đặt để khắc phục ›", color = C.Orange, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp).clickable {
                                runCatching { ctx.startActivity(Intent(act)) }
                            }
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        PrimaryButton("Vẫn vào app", modifier = Modifier.fillMaxWidth()) {
            onContinue(if (dontAsk) items.map { it.id }.toSet() else emptySet())
        }
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { dontAsk = !dontAsk }.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(22.dp).clip(RoundedCornerShape(6.dp))
                    .then(if (dontAsk) Modifier.background(C.Green) else Modifier.border(2.dp, C.TickOff, RoundedCornerShape(6.dp))),
                contentAlignment = Alignment.Center
            ) { if (dontAsk) Icon(Icons.Filled.Check, null, tint = C.OnGreen, modifier = Modifier.size(16.dp)) }
            Spacer(Modifier.width(10.dp))
            Text("Không hỏi lại các điểm này nữa", color = C.Sub2, fontSize = 14.sp)
        }
    }
}

@Composable
private fun BlockedScreen(items: List<SecItem>) {
    val ctx = LocalContext.current
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(40.dp))
        GlowLogo(C.Red, Color(0xFFFF9A6A), spin = false)
        Spacer(Modifier.height(18.dp))
        Text("App không an toàn", color = C.Red, fontSize = 25.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        for (i in items) {
            Text(i.title, color = C.Text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            Text(i.detail, color = C.Sub2, fontSize = 14.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
        }
        Text("Để bảo vệ danh bạ, app sẽ không mở.", color = C.Sub2, fontSize = 14.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(22.dp))
        Text("Gọi khẩn cấp vẫn dùng được:", color = C.Sub2, fontSize = 14.sp)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            for ((n, label) in listOf("113" to "Công an", "114" to "Cứu hỏa", "115" to "Cấp cứu")) {
                Column(
                    Modifier.width(92.dp).clip(RoundedCornerShape(16.dp)).background(C.Red.copy(alpha = 0.15f))
                        .border(2.dp, C.Red, RoundedCornerShape(16.dp))
                        .clickable { runCatching { ctx.startActivity(Intent(Intent.ACTION_DIAL, "tel:$n".toUri())) } }
                        .padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(n, color = C.Red, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                    Text(label, color = C.Sub2, fontSize = 12.sp)
                }
            }
        }
    }
}

/** Màn ☰ → Kiểm tra bảo mật: xem chi tiết, kiểm tra lại, xem mã con dấu. */
@Composable
fun SecurityScreen(data: AppData, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var report by remember { mutableStateOf<SecReport?>(null) }
    fun recheck() {
        report = null
        scope.launch { report = withContext(Dispatchers.IO) { SecurityCheck.run(ctx) } }
    }
    LaunchedEffect(Unit) { recheck() }
    Column(Modifier.fillMaxSize()) {
        TopBar("Kiểm tra bảo mật", onBack)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val r = report
            if (r == null) {
                Text("Đang kiểm tra…", color = C.Sub2)
            } else {
                val (title, color) = when {
                    r.danger.isNotEmpty() -> "App không an toàn" to C.Red
                    r.warnings.isNotEmpty() -> "Có ${r.warnings.size} điểm cần lưu ý" to C.Orange
                    else -> "Thiết bị an toàn" to SAFE_GREEN
                }
                Text(title, color = color, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                CardBox {
                    for (i in r.items) {
                        Row(Modifier.padding(vertical = 6.dp)) {
                            Text(
                                when (i.level) { SecLevel.OK -> "✅"; SecLevel.WARN -> "⚠️"; SecLevel.DANGER -> "⛔" },
                                fontSize = 16.sp
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(i.title, color = C.Text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                if (i.detail.isNotEmpty()) Text(i.detail, color = C.Sub2, fontSize = 13.sp)
                                i.settingsAction?.let { act ->
                                    Text(
                                        "Mở Cài đặt ›", color = C.Blue, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(top = 2.dp).clickable { runCatching { ctx.startActivity(Intent(act)) } }
                                    )
                                }
                            }
                        }
                    }
                }
                PrimaryButton("Kiểm tra lại", modifier = Modifier.fillMaxWidth()) { recheck() }
                if (data.prefs.ignoredSecurity.isNotEmpty()) {
                    SecondaryButton("Hỏi lại các cảnh báo đã ẩn", Modifier.fillMaxWidth()) {
                        data.prefs.ignoredSecurity = emptySet()
                        Actions.toast(ctx, "Lần mở app sau sẽ hỏi lại các cảnh báo")
                    }
                }
                // Mã con dấu – dùng để cài kiểm tra "app chính chủ"
                CardBox {
                    Text("Mã con dấu của bản app này", color = C.Text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text(r.signature.ifEmpty { "Không đọc được" }, color = C.Sub2, fontSize = 12.sp)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier.clip(RoundedCornerShape(12.dp)).background(C.Search)
                            .clickable { Actions.copyText(ctx, r.signature, "Đã sao chép mã con dấu") }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.ContentCopy, null, tint = C.Blue, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Sao chép mã", color = C.Blue, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("Mã này không phải bí mật. Gửi mã cho người lập trình để bật kiểm tra \"app chính chủ\".", color = C.Sub, fontSize = 12.sp)
                }
                Text(
                    "Lưu ý: đây không phải phần mềm diệt virus. App chỉ phát hiện các dấu hiệu rủi ro phổ biến. Để quét virus hãy dùng Google Play Protect (CH Play → ảnh đại diện → Play Protect).",
                    color = C.Sub, fontSize = 12.sp
                )
            }
        }
    }
}
