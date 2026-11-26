package com.quoctuan.danhbaso.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quoctuan.danhbaso.BuildConfig
import com.quoctuan.danhbaso.util.Actions
import com.quoctuan.danhbaso.util.GITHUB_RELEASES_URL

@Composable
private fun AppHeader() {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        // Logo app (mẫu A: navy + viền phát sáng), cắt bo góc như trên màn hình chính
        Box(Modifier.size(84.dp).clip(RoundedCornerShape(22.dp))) {
            val m = Modifier.requiredSize(126.dp)
            androidx.compose.foundation.Image(androidx.compose.ui.res.painterResource(com.quoctuan.danhbaso.R.drawable.ic_launcher_background), null, m.align(Alignment.Center))
            androidx.compose.foundation.Image(androidx.compose.ui.res.painterResource(com.quoctuan.danhbaso.R.drawable.ic_launcher_foreground), null, m.align(Alignment.Center))
        }
        Gap(8)
        Text("Danh bạ số", color = C.Text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("Phiên bản ${BuildConfig.VERSION_NAME}", color = C.Sub, fontSize = 14.sp)
    }
}

/** Màn Thông tin ứng dụng. */
@Composable
fun AppInfoScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        TopBar("Thông tin ứng dụng", onBack)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            AppHeader()
            CardBox {
                val rows = listOf(
                    "Phát triển bởi" to "Quốc Tuấn",
                    "Phiên bản" to BuildConfig.VERSION_NAME,
                    "Ngày phát hành" to BuildConfig.RELEASE_DATE,
                    "Mã nguồn" to "GitHub",
                )
                rows.forEachIndexed { i, (k, v) ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                        Text(k, color = C.Sub, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        Text(v, color = C.Text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                    if (i < rows.lastIndex) HorizontalDivider(color = C.Line)
                }
            }
            CardBox {
                Text("Giới thiệu", color = C.Text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Gap(6)
                Text(
                    "Danh bạ số là app gọi điện và quản lý danh bạ, có sao lưu danh bạ bằng file mã hóa và khôi phục cùng lúc vào SIM và Gmail khi đổi máy hoặc mất máy.",
                    color = C.Sub2, fontSize = 14.sp, lineHeight = 21.sp
                )
            }
            CardBox {
                Text("Tính năng chính", color = C.Text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Gap(6)
                for (f in listOf(
                    "Lịch sử cuộc gọi: mỗi số 1 dòng, trạng thái có màu",
                    "Danh bạ dùng chung với điện thoại, mở Zalo nhanh",
                    "Lưu số vào SIM và Gmail cùng lúc, đổi Gmail dễ dàng",
                    "Sao lưu danh bạ bằng file mã hóa AES-256",
                    "Khôi phục vào SIM + Gmail khi đổi máy",
                )) {
                    Row(Modifier.padding(vertical = 3.dp)) {
                        Box(Modifier.padding(top = 7.dp).size(6.dp).clip(CircleShape).background(C.Blue))
                        Spacer(Modifier.width(10.dp))
                        Text(f, color = C.Sub2, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

/** Màn Cập nhật ứng dụng: mở trang GitHub Releases bằng trình duyệt (app không có quyền Internet). */
@Composable
fun UpdateScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    Column(Modifier.fillMaxSize()) {
        TopBar("Cập nhật ứng dụng", onBack)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            CardBox { AppHeader() }
            Button(
                onClick = { Actions.openUrl(ctx, GITHUB_RELEASES_URL) },
                modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = C.Blue, contentColor = C.OnBlue)
            ) {
                Icon(Icons.AutoMirrored.Filled.OpenInNew, null)
                Spacer(Modifier.width(8.dp))
                Text("Mở trang GitHub để xem bản mới", fontWeight = FontWeight.Bold)
            }
            Text(GITHUB_RELEASES_URL.removePrefix("https://"), color = C.Sub, fontSize = 12.sp, modifier = Modifier.fillMaxWidth())
            CardBox {
                Text("Cách cập nhật", color = C.Text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Gap(8)
                val steps = listOf(
                    "Bấm nút trên để mở trang GitHub. Xem bản mới nhất có số lớn hơn ${BuildConfig.VERSION_NAME} không.",
                    "Tải file APK của bản mới về máy.",
                    "Mở file APK và bấm Cập nhật. Cài đè lên bản cũ, danh bạ và cài đặt vẫn giữ nguyên.",
                )
                steps.forEachIndexed { i, s ->
                    Row(Modifier.padding(vertical = 5.dp)) {
                        Box(Modifier.size(26.dp).clip(CircleShape).background(C.BlueBg), contentAlignment = Alignment.Center) {
                            Text("${i + 1}", color = C.Blue, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(s, color = C.Sub2, fontSize = 14.sp, lineHeight = 20.sp)
                    }
                }
            }
        }
    }
}
