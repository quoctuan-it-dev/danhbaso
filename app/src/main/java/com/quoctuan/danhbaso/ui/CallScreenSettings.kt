package com.quoctuan.danhbaso.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.quoctuan.danhbaso.call.CallRole
import com.quoctuan.danhbaso.util.Actions

/** ☰ → Màn hình gọi: dùng màn hình gọi của điện thoại hay của Danh bạ số. */
@Composable
fun CallScreenSettings(onBack: () -> Unit) {
    val ctx = LocalContext.current
    var isApp by remember { mutableStateOf(CallRole.isDefault(ctx)) }

    // Quay lại từ Cài đặt thì đọc lại
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) isApp = CallRole.isDefault(ctx) }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }

    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val roleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        isApp = CallRole.isDefault(ctx)
        // Cần quyền thông báo để hiện cuộc gọi đến khi đang dùng app khác
        if (isApp && Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    Column(Modifier.fillMaxSize()) {
        TopBar("Màn hình gọi", onBack)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Chọn màn hình hiện ra khi bạn gọi đi và khi có người gọi đến.", color = C.Sub2, fontSize = 14.sp)
            OptionCard(
                on = !isApp, title = "Của điện thoại",
                sub = "Như hiện tại. Danh bạ số chỉ bấm số, cuộc gọi do app Điện thoại của máy lo."
            ) {
                if (isApp) runCatching { ctx.startActivity(CallRole.settingsIntent()) }
                    .onFailure { Actions.toast(ctx, "Vào Cài đặt → Ứng dụng → Ứng dụng mặc định → Điện thoại") }
            }
            OptionCard(
                on = isApp, title = "Của Danh bạ số",
                sub = "Gọi đi, nghe máy, tắt máy… ngay trong app. Vuốt để nghe, có nút Loa ngay trên thông báo."
            ) {
                if (!isApp) runCatching { roleLauncher.launch(CallRole.requestIntent(ctx)) }
                    .onFailure { Actions.toast(ctx, "Máy này không cho đổi ứng dụng Điện thoại") }
            }
            if (isApp) {
                Text(
                    "Muốn quay về màn hình gọi của máy: bấm \"Của điện thoại\", app mở Cài đặt → chọn lại app Điện thoại của máy.",
                    color = C.Sub, fontSize = 12.5.sp
                )
            }
            CardBox {
                Text("Vẫn giữ nguyên", color = C.Text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(Modifier.padding(2.dp))
                for (l in listOf(
                    "Gọi khẩn cấp 112 / 113 / 114 / 115",
                    "Nhạc chuông, rung do máy lo",
                    "Lịch sử cuộc gọi máy tự ghi",
                    "Zalo, Messenger gọi vẫn dùng màn hình riêng của chúng",
                )) Text("• $l", color = C.Sub2, fontSize = 13.5.sp)
            }
        }
    }
}

@Composable
private fun OptionCard(on: Boolean, title: String, sub: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(C.Card)
            .border(2.dp, if (on) C.Green else Color.Transparent, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            Modifier.padding(top = 2.dp).size(22.dp).clip(CircleShape)
                .border(2.dp, if (on) C.Green else C.TickOff, CircleShape),
            contentAlignment = Alignment.Center
        ) { if (on) Box(Modifier.size(11.dp).clip(CircleShape).background(C.Green)) }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, color = if (on) C.Green else C.Text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(sub, color = C.Sub2, fontSize = 13.sp)
        }
    }
}
