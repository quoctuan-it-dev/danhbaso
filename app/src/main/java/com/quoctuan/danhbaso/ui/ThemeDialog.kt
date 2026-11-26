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
                    "Bấm là đổi ngay. Chế độ Sáng/Tối vẫn theo điện thoại.",
                    color = C.Sub, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp)
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Xong", color = C.Blue) } }
    )
}
