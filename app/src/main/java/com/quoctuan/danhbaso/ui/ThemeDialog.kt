package com.quoctuan.danhbaso.ui

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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quoctuan.danhbaso.data.AppData

/** Hộp chọn màu giao diện: bấm vào màu nào là đổi ngay, lần sau mở app vẫn giữ. */
@Composable
fun ThemeDialog(data: AppData, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = C.Nav,
        title = { Text("Màu giao diện", color = C.Text) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Sáng / Tối
                Text("Sáng / Tối", color = C.Blue, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val labels = listOf("📱 Tự động", "☀️ Sáng", "🌙 Tối")
                    labels.forEachIndexed { i, label ->
                        val on = data.darkMode == i
                        val shape = RoundedCornerShape(12.dp)
                        Box(
                            Modifier.weight(1f).clip(shape)
                                .background(if (on) C.BlueBg else C.Card)
                                .then(if (on) Modifier.border(2.dp, C.Green, shape) else Modifier.border(1.dp, C.Border, shape))
                                .clickable { data.changeDarkMode(i) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                label, color = if (on) C.Text else C.Sub2, fontSize = 14.sp,
                                fontWeight = if (on) FontWeight.Bold else FontWeight.Normal, maxLines = 1
                            )
                        }
                    }
                }
                Text(
                    if (data.darkMode == 0) "Tự động: điện thoại sáng thì app sáng, điện thoại tối thì app tối." else MODE_NAMES[data.darkMode],
                    color = C.Sub, fontSize = 12.sp
                )
                Spacer(Modifier.height(6.dp))
                Text("Bộ màu", color = C.Blue, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                THEME_NAMES.forEachIndexed { i, name ->
                    val sel = data.theme == i
                    val shape = RoundedCornerShape(14.dp)
                    Row(
                        Modifier.fillMaxWidth().clip(shape)
                            .background(if (sel) C.BlueBg else C.Card)
                            .then(if (sel) Modifier.border(2.dp, C.Green, shape) else Modifier)
                            .clickable { data.changeTheme(i) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val (accent, bg) = THEME_SWATCH[i]
                        Box(
                            Modifier.size(40.dp).clip(CircleShape)
                                .background(Brush.linearGradient(0f to accent, 0.48f to accent, 0.52f to bg, 1f to bg))
                        )
                        Spacer(Modifier.width(14.dp))
                        Text(
                            name, color = C.Text, fontSize = 18.sp,
                            fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.weight(1f)
                        )
                        if (sel) Icon(Icons.Filled.Check, "Đang dùng", tint = C.Green, modifier = Modifier.size(24.dp))
                    }
                }
                Text(
                    "Bấm là đổi ngay. Mỗi bộ màu đều có bản sáng và bản tối.",
                    color = C.Sub, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp)
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Xong", color = C.Blue) } }
    )
}

/** Hộp chọn cỡ chữ toàn app: Nhỏ / Vừa / Lớn / Rất lớn, mỗi dòng có chữ xem trước. */
@Composable
fun FontSizeDialog(data: AppData, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = C.Nav,
        title = { Text("Cỡ chữ toàn app", color = C.Text) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                com.quoctuan.danhbaso.data.FONT_NAMES.forEachIndexed { i, name ->
                    val sel = data.fontSize == i
                    val shape = RoundedCornerShape(14.dp)
                    // Chữ xem trước tính theo cỡ của lựa chọn, so với cỡ đang dùng
                    val k = com.quoctuan.danhbaso.data.FONT_FACTORS[i] / data.fontFactor
                    Row(
                        Modifier.fillMaxWidth().clip(shape)
                            .background(if (sel) C.BlueBg else C.Card)
                            .then(if (sel) Modifier.border(2.dp, C.Green, shape) else Modifier)
                            .clickable { data.changeFontSize(i) }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(name, color = C.Text, fontSize = (16 * k).sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
                            Text("Mẹ Kiều · 0778 638 687", color = C.Sub, fontSize = (13 * k).sp, maxLines = 1)
                        }
                        if (sel) Icon(Icons.Filled.Check, "Đang dùng", tint = C.Green, modifier = Modifier.size(24.dp))
                    }
                }
                Text(
                    "Bấm là đổi ngay cho mọi màn hình. Bàn phím số vẫn chỉnh riêng ở \"Cỡ bàn phím\".",
                    color = C.Sub, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp)
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Xong", color = C.Blue) } }
    )
}
