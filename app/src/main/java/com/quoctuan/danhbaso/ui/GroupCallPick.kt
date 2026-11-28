package com.quoctuan.danhbaso.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quoctuan.danhbaso.call.GroupCall
import com.quoctuan.danhbaso.data.AppData
import com.quoctuan.danhbaso.util.Actions
import com.quoctuan.danhbaso.util.TextUtil

/** Chọn người để gọi nhóm (tối đa 5). [preselect] = chọn sẵn (VD thành viên của một nhóm). */
@Composable
fun GroupCallPickScreen(data: AppData, preselect: List<Long>, title: String?, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val people = remember(data.visibleContacts) { data.visibleContacts.filter { it.phones.isNotEmpty() } }
    var chosen by remember { mutableStateOf(preselect.filter { id -> people.any { it.id == id } }.take(GroupCall.MAX_PEOPLE)) }
    var query by remember { mutableStateOf("") }
    val shown = remember(people, query) {
        if (query.isBlank()) people else {
            val q = TextUtil.removeDiacritics(query).lowercase().trim()
            val qd = query.filter { it.isDigit() }
            people.filter { it.searchKey.contains(q) || (qd.isNotEmpty() && it.digits.contains(qd)) }
        }
    }
    // Người đã chọn lên đầu cho dễ thấy
    val ordered = remember(shown, chosen) { shown.sortedByDescending { it.id in chosen } }

    Column(Modifier.fillMaxSize()) {
        TopBar(if (title != null) "Gọi nhóm: $title" else "Chọn người gọi nhóm", onBack)
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 4.dp).fillMaxWidth().heightIn(min = 48.dp)
                .clip(RoundedCornerShape(24.dp)).background(C.Search).padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Search, null, tint = C.Sub2)
            Spacer(Modifier.width(10.dp))
            Box(Modifier.weight(1f)) {
                if (query.isEmpty()) Text("Tìm người liên hệ", color = C.Sub, fontSize = 15.sp)
                BasicTextField(
                    value = query, onValueChange = { query = it }, singleLine = true,
                    textStyle = TextStyle(color = C.Text, fontSize = 15.sp), cursorBrush = SolidColor(C.Blue),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        Text(
            "Đã chọn ${chosen.size}/${GroupCall.MAX_PEOPLE} · app sẽ gọi lần lượt rồi gộp vào nói chuyện chung",
            color = C.Sub2, fontSize = 12.5.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
        )
        Box(Modifier.weight(1f)) {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 96.dp)) {
                items(ordered, key = { it.id }) { c ->
                    val on = c.id in chosen
                    Row(
                        Modifier.padding(vertical = 4.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp))
                            .background(if (on) C.Green.copy(alpha = 0.12f) else C.Card)
                            .clickable {
                                chosen = when {
                                    on -> chosen - c.id
                                    chosen.size >= GroupCall.MAX_PEOPLE -> {
                                        Actions.toast(ctx, "Tối đa ${GroupCall.MAX_PEOPLE} người"); chosen
                                    }
                                    else -> chosen + c.id
                                }
                            }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Avatar(c.name, 40.dp, letter = c.letter)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(c.name, color = C.Text, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(c.phones.first(), color = C.Sub2, fontSize = 12.5.sp)
                        }
                        Box(
                            Modifier.size(26.dp).clip(RoundedCornerShape(8.dp))
                                .background(if (on) C.Green else Color.Transparent)
                                .border(2.dp, if (on) C.Green else C.TickOff, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) { if (on) Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(18.dp)) }
                    }
                }
            }
            // Nút gọi to ở dưới
            Row(
                Modifier.align(Alignment.BottomCenter).padding(16.dp).fillMaxWidth().height(58.dp)
                    .clip(RoundedCornerShape(29.dp))
                    .background(
                        if (chosen.isEmpty()) Brush.linearGradient(listOf(C.Sub, C.Sub))
                        else Brush.linearGradient(listOf(Color(0xFF55E39A), Color(0xFF1AA95E)))
                    )
                    .clickable(enabled = chosen.isNotEmpty()) {
                        val list = chosen.mapNotNull { id -> people.firstOrNull { it.id == id } }
                            .map { it.name to it.phones.first() }
                        val err = GroupCall.start(ctx, title, list)
                        if (err != null) Actions.toast(ctx, err) else onBack()
                    },
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Call, null, tint = Color.White, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    if (chosen.isEmpty()) "Chọn người để gọi" else "Gọi nhóm ${chosen.size} người",
                    color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}
