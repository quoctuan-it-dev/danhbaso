package com.quoctuan.danhbaso.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quoctuan.danhbaso.data.AppData
import com.quoctuan.danhbaso.data.ContactItem
import com.quoctuan.danhbaso.data.SimWriteResult
import com.quoctuan.danhbaso.util.Actions
import com.quoctuan.danhbaso.util.PhoneUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Thêm số mới ([existing] = null) hoặc Sửa liên hệ.
 * Hai ô "Lưu vào SIM" / "Lưu vào Gmail": bấm để chọn, cả ô viền xanh + dấu tích.
 * Nút Lưu ở góc trên bên phải.
 */
@Composable
fun EditContactScreen(
    data: AppData,
    existing: ContactItem?,
    prefillNumber: String?,
    onClose: () -> Unit,
    onSaved: () -> Unit,
    onPickAccount: () -> Unit,
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val account = data.selectedAccount
    val simOk = data.simInfo.available

    var name by remember { mutableStateOf(existing?.name ?: "") }
    var phone by remember { mutableStateOf(existing?.firstPhone ?: prefillNumber ?: "") }
    val wasInGmail = existing != null && account != null && account in existing.googleAccounts
    val wasInSim = existing?.inSim == true
    var toSim by remember { mutableStateOf(if (existing == null) simOk else wasInSim) }
    var toGmail by remember { mutableStateOf(if (existing == null) account != null else wasInGmail) }
    var saving by remember { mutableStateOf(false) }

    val canSave = !saving && name.isNotBlank() && PhoneUtil.local(phone).length >= 3 &&
        (toSim || toGmail || (existing != null && !existing.simOnly))

    fun save() {
        saving = true
        scope.launch {
            val msg = withContext(Dispatchers.IO) { doSave(data, existing, name.trim(), phone.trim(), toSim, toGmail, wasInSim, wasInGmail) }
            saving = false
            Actions.toast(ctx, msg)
            onSaved()
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 12.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) { Icon(Icons.Filled.Close, "Đóng", tint = C.Text) }
            Text(if (existing == null) "Thêm số mới" else "Sửa liên hệ", color = C.Text, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Button(
                onClick = { save() }, enabled = canSave, shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = C.Blue, contentColor = C.OnBlue,
                    disabledContainerColor = C.Card2, disabledContentColor = C.Sub
                ),
                contentPadding = PaddingValues(horizontal = 18.dp)
            ) {
                if (saving) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = C.OnBlue)
                else Text("Lưu", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(Modifier.size(88.dp).clip(CircleShape).background(C.Search), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Person, null, tint = C.Sub, modifier = Modifier.size(44.dp))
                }
            }
            val fieldColors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = C.Text, unfocusedTextColor = C.Text,
                focusedBorderColor = C.Blue, unfocusedBorderColor = C.Border,
                focusedContainerColor = C.Card, unfocusedContainerColor = C.Card,
            )
            OutlinedTextField(
                value = name, onValueChange = { name = it }, label = { Text("Tên") }, singleLine = true,
                // Tự viết hoa chữ đầu mỗi từ: gõ "anh hai" -> "Anh Hai"
                keyboardOptions = KeyboardOptions(capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = fieldColors
            )
            OutlinedTextField(
                value = phone, onValueChange = { phone = it }, label = { Text("Số điện thoại") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = fieldColors
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Lưu vào ", color = C.Text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text("(bấm vào ô để chọn hoặc bỏ chọn)", color = C.Sub, fontSize = 12.sp)
            }
            SelectCard(
                selected = toSim, enabled = simOk, title = "Lưu vào SIM",
                subtitle = when {
                    !simOk -> "Không đọc được SIM trên máy này"
                    data.simInfo.free >= 0 -> "SIM 1 · còn trống ${data.simInfo.free} chỗ"
                    else -> "SIM 1"
                },
                icon = Icons.Filled.SimCard, onClick = { toSim = !toSim }
            )
            SelectCard(
                selected = toGmail, enabled = account != null, title = "Lưu vào Gmail",
                subtitle = if (account == null) "Chưa có Gmail trên máy – bấm để thêm" else "$account · đổi ở vòng tròn góc trên",
                icon = Icons.Filled.Email,
                onClick = { toGmail = !toGmail },
                subtitleClick = if (account == null) ({ Actions.addGoogleAccount(ctx) }) else null
            )
            if (!toSim && !toGmail && (existing == null || existing.simOnly)) {
                Text("Hãy chọn ít nhất một nơi để lưu", color = C.Orange, fontSize = 13.sp, modifier = Modifier.fillMaxWidth())
            }
            if (toSim) {
                Text("SIM chỉ lưu được tên ngắn (khoảng 14 ký tự, có thể mất dấu). Gmail giữ nguyên tên đầy đủ.", color = C.Sub, fontSize = 12.sp)
            }
        }
    }
}

/** Thực hiện lưu (chạy nền). Trả về câu thông báo. */
private fun doSave(
    data: AppData, existing: ContactItem?, name: String, phone: String,
    toSim: Boolean, toGmail: Boolean, wasInSim: Boolean, wasInGmail: Boolean,
): String {
    val account = data.selectedAccount
    val notes = ArrayList<String>()
    fun simInsert() {
        when (data.simRepo.insert(name, phone)) {
            SimWriteResult.OK -> notes += "SIM"
            SimWriteResult.SHORTENED -> notes += "SIM (tên rút gọn)"
            SimWriteResult.FULL -> notes += "SIM đầy, chưa lưu"
            SimWriteResult.NO_SIM -> notes += "không thấy SIM"
            SimWriteResult.ERROR -> notes += "máy không cho ghi SIM"
        }
    }

    if (existing == null) {
        if (toGmail && account != null) {
            notes += if (data.contactsRepo.insertGoogle(account, name, listOf(phone), false)) "Gmail" else "lỗi lưu Gmail"
        }
        if (toSim) simInsert()
        return "Đã lưu: " + notes.joinToString(", ")
    }

    val old = existing.firstPhone
    val changed = name != existing.name || PhoneUtil.key(phone) != PhoneUtil.key(old)

    if (existing.simOnly) {
        if (toSim && changed) {
            data.simRepo.deleteNumber(old); simInsert()
        } else if (!toSim) data.simRepo.deleteNumber(old)
        if (toGmail && account != null) {
            notes += if (data.contactsRepo.insertGoogle(account, name, listOf(phone), false)) "Gmail" else "lỗi lưu Gmail"
        }
        return "Đã cập nhật " + notes.joinToString(", ")
    }

    if (changed) data.contactsRepo.updateNameAndNumber(existing.id, name, old, phone)
    // Gmail
    if (account != null) {
        if (toGmail && !wasInGmail) {
            notes += if (data.contactsRepo.insertGoogle(account, name, listOf(phone), existing.starred)) "thêm vào Gmail" else "lỗi lưu Gmail"
        } else if (!toGmail && wasInGmail) {
            data.contactsRepo.deleteFromAccount(existing.id, account)
            notes += "bỏ khỏi Gmail"
        }
    }
    // SIM
    if (toSim && !wasInSim) simInsert()
    else if (!toSim && wasInSim) {
        data.simRepo.deleteNumber(old); notes += "bỏ khỏi SIM"
    } else if (toSim && wasInSim && changed) {
        data.simRepo.deleteNumber(old); simInsert()
    }
    return if (notes.isEmpty()) "Đã lưu thay đổi" else "Đã lưu thay đổi: " + notes.joinToString(", ")
}
