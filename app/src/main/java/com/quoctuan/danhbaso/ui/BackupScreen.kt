package com.quoctuan.danhbaso.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import android.provider.OpenableColumns
import android.view.WindowManager
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quoctuan.danhbaso.data.AppData
import com.quoctuan.danhbaso.data.BackupContact
import com.quoctuan.danhbaso.data.Vcf
import com.quoctuan.danhbaso.data.BackupCrypto
import com.quoctuan.danhbaso.data.RestoreResult
import com.quoctuan.danhbaso.util.Actions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

/** Màn Sao lưu và khôi phục (vào từ menu ☰). Chặn chụp màn hình khi đang ở màn này. */
@Composable
fun BackupScreen(data: AppData, onBack: () -> Unit, onChanged: () -> Unit) {
    val ctx = LocalContext.current
    // Chặn chụp / quay màn hình khi đang ở màn sao lưu
    DisposableEffect(Unit) {
        val w = ctx.findActivity()?.window
        w?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose { w?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
    }
    var tab by remember { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        TopBar("Sao lưu và khôi phục", onBack)
        Row(
            Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(C.Card).padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for ((i, t) in listOf("Sao lưu", "Khôi phục", "File VCF").withIndex()) {
                Box(
                    Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(10.dp))
                        .background(if (tab == i) C.Chip else Color.Transparent).clickable { tab = i },
                    contentAlignment = Alignment.Center
                ) { Text(t, color = if (tab == i) C.Text else C.Sub2, fontWeight = if (tab == i) FontWeight.SemiBold else FontWeight.Normal) }
            }
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            when (tab) {
                0 -> BackupTab(data)
                1 -> RestoreTab(data, onChanged)
                else -> VcfTab(data, onChanged)
            }
        }
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = C.Text, unfocusedTextColor = C.Text,
    focusedBorderColor = C.Blue, unfocusedBorderColor = C.Border,
    focusedContainerColor = C.Bg, unfocusedContainerColor = C.Bg,
)

@Composable
private fun PasswordField(value: String, onChange: (String) -> Unit, label: String) {
    var show by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label) }, singleLine = true,
        visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            IconButton(onClick = { show = !show }) {
                Icon(if (show) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, "Hiện mật khẩu", tint = C.Sub2)
            }
        },
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = fieldColors()
    )
}

@Composable
private fun BackupTab(data: AppData) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var pw1 by remember { mutableStateOf("") }
    var pw2 by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var savedUri by remember { mutableStateOf<Uri?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val strength = BackupCrypto.strength(pw1)
    val fileName = "danhba_" + SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) + ".dbat"

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        busy = true
        error = null
        val password = pw1.toCharArray()
        scope.launch {
            try {
                val n = withContext(Dispatchers.IO) { data.backup.writeBackup(uri, data.contacts, password) }
                data.markBackup(fileName)
                savedUri = uri
                pw1 = ""
                pw2 = ""
                Actions.toast(ctx, "Đã sao lưu $n liên hệ")
            } catch (e: Exception) {
                error = "Không lưu được file: ${e.message ?: "lỗi không rõ"}"
            } finally {
                password.fill(' ')
                busy = false
            }
        }
    }

    CardBox {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Tạo file sao lưu", color = C.Text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text("${data.contacts.size} liên hệ", color = C.Sub, fontSize = 13.sp)
        }
        Gap(12)
        PasswordField(pw1, { pw1 = it }, "Mật khẩu file (ít nhất 8 ký tự)")
        Gap(6)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f).height(6.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                for (i in 1..4) Box(
                    Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp))
                        .background(if (i <= strength) strengthColor(strength) else C.Line)
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(strengthLabel(pw1, strength), color = strengthColor(strength), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
        Gap(10)
        PasswordField(pw2, { pw2 = it }, "Nhập lại mật khẩu")
        if (pw2.isNotEmpty() && pw1 != pw2) Text("Hai mật khẩu chưa giống nhau", color = C.Red, fontSize = 12.sp)
        Gap(8)
        Text("Quên mật khẩu là không mở được file. Hãy ghi mật khẩu ra giấy và cất kỹ.", color = C.Orange, fontSize = 12.sp)
        Gap(12)
        Button(
            onClick = { launcher.launch(fileName) },
            enabled = !busy && pw1.length >= 8 && pw1 == pw2 && data.contacts.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = C.Blue, contentColor = C.OnBlue)
        ) {
            if (busy) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = C.OnBlue)
                Spacer(Modifier.width(8.dp))
                Text("Đang mã hóa…")
            } else {
                Icon(Icons.Filled.Lock, null)
                Spacer(Modifier.width(8.dp))
                Text("Mã hóa và lưu file", fontWeight = FontWeight.Bold)
            }
        }
        Gap(6)
        Text("Ở bước chọn nơi lưu, bạn có thể chọn thẳng Google Drive.", color = C.Sub, fontSize = 12.sp)
        error?.let { Gap(6); Text(it, color = C.Red, fontSize = 13.sp) }
        savedUri?.let { uri ->
            Gap(10)
            OutlinedButton(onClick = { Actions.shareFile(ctx, uri) }, modifier = Modifier.fillMaxWidth().height(44.dp), shape = RoundedCornerShape(14.dp)) {
                Icon(Icons.Filled.Share, null, tint = C.Text)
                Spacer(Modifier.width(8.dp))
                Text("Chia sẻ lên Google Drive", color = C.Text)
            }
        }
        Gap(10)
        val last = data.lastBackup
        Text(
            if (last == 0L) "Chưa sao lưu lần nào"
            else "Lần sao lưu gần nhất: " + SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(last)) +
                (data.prefs.lastBackupName?.let { " · $it" } ?: ""),
            color = C.Sub, fontSize = 12.sp
        )
    }
}

private fun strengthColor(s: Int) = when (s) {
    0 -> C.Red
    1 -> C.Orange
    else -> C.Green
}

private fun strengthLabel(pw: String, s: Int) = when {
    pw.isEmpty() -> ""
    s == 0 -> "Quá ngắn"
    s == 1 -> "Tạm được"
    s == 2 -> "Khá mạnh"
    s == 3 -> "Mạnh"
    else -> "Rất mạnh"
}

@Composable
private fun RestoreTab(data: AppData, onChanged: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var fileUri by remember { mutableStateOf<Uri?>(null) }
    var fileLabel by remember { mutableStateOf("") }
    var pw by remember { mutableStateOf("") }
    val account = data.selectedAccount
    var toGmail by remember { mutableStateOf(account != null) }
    var toSim by remember { mutableStateOf(data.simInfo.available) }
    var busy by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    var status by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var result by remember { mutableStateOf<RestoreResult?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            fileUri = uri
            fileLabel = describeFile(ctx, uri)
            result = null
            error = null
        }
    }

    CardBox {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(C.Bg).clickable { picker.launch(arrayOf("*/*")) }.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Description, null, tint = C.Blue)
            Spacer(Modifier.width(12.dp))
            Text(if (fileUri == null) "Chọn file sao lưu (.dbat)" else fileLabel, color = C.Text, fontSize = 14.sp, modifier = Modifier.weight(1f))
            Text(if (fileUri == null) "Chọn" else "Đổi file", color = C.Blue, fontSize = 13.sp)
        }
        Gap(10)
        PasswordField(pw, { pw = it }, "Mật khẩu file")
        Gap(12)
        Text("Lưu cùng lúc vào", color = C.Text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Gap(8)
        SelectCard(
            selected = toGmail, enabled = account != null,
            title = "Gmail", subtitle = account ?: "Chưa có Gmail trên máy",
            icon = Icons.Filled.Email, onClick = { toGmail = !toGmail }
        )
        Gap(8)
        SelectCard(
            selected = toSim, enabled = data.simInfo.available,
            title = "SIM 1",
            subtitle = when {
                !data.simInfo.available -> "Không đọc được SIM"
                data.simInfo.maxRecords > 0 -> "Còn trống ${data.simInfo.free} / ${data.simInfo.maxRecords} chỗ"
                else -> "SIM đang dùng"
            },
            icon = Icons.Filled.SimCard, onClick = { toSim = !toSim }
        )
        Gap(12)
        Button(
            onClick = {
                val uri = fileUri ?: return@Button
                busy = true; error = null; result = null; progress = 0f
                val password = pw.toCharArray()
                val acc = if (toGmail) account else null
                val sim = toSim
                scope.launch {
                    try {
                        status = "Đang giải mã…"
                        val list: List<BackupContact> = withContext(Dispatchers.IO) { data.backup.readBackup(uri, password) }
                        status = "Đang lưu ${list.size} liên hệ…"
                        val r = withContext(Dispatchers.IO) {
                            data.backup.restore(list, acc, sim) { i, n -> progress = i.toFloat() / n }
                        }
                        result = r
                        pw = ""
                        onChanged()
                    } catch (e: BackupCrypto.WrongPasswordException) {
                        error = "Sai mật khẩu hoặc file đã bị sửa."
                    } catch (e: BackupCrypto.BadFileException) {
                        error = "File này không phải file sao lưu của Danh bạ số."
                    } catch (e: Exception) {
                        error = "Không đọc được file: ${e.message ?: "lỗi không rõ"}"
                    } finally {
                        password.fill(' ')
                        busy = false
                    }
                }
            },
            enabled = !busy && fileUri != null && pw.isNotEmpty() && (toGmail || toSim),
            modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = C.Blue, contentColor = C.OnBlue)
        ) {
            Icon(Icons.Filled.FileDownload, null)
            Spacer(Modifier.width(8.dp))
            Text("Giải mã và khôi phục", fontWeight = FontWeight.Bold)
        }
        if (busy) {
            Gap(10)
            Text(status, color = C.Sub2, fontSize = 13.sp)
            Gap(6)
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
        }
        error?.let { Gap(8); Text(it, color = C.Red, fontSize = 13.sp) }
    }

    result?.let { r ->
        CardBox(bg = C.GreenBg) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.CheckCircle, null, tint = C.Green)
                Spacer(Modifier.width(8.dp))
                Text("Khôi phục xong (${r.total} liên hệ trong file)", color = C.Green, fontWeight = FontWeight.Bold)
            }
            Gap(8)
            if (account != null && toGmail) {
                ResultLine("Gmail: lưu mới", "${r.googleAdded} số")
                ResultLine("Gmail: đã có sẵn, bỏ qua", "${r.googleSkipped} số")
                if (r.googleFailed > 0) ResultLine("Gmail: lỗi", "${r.googleFailed} số", C.Red)
            }
            if (toSim) {
                ResultLine("SIM: lưu mới", "${r.simAdded} số")
                ResultLine("SIM: đã có sẵn, bỏ qua", "${r.simSkipped} số")
                if (r.simShortened > 0) ResultLine("SIM: tên bị rút gọn", "${r.simShortened} số")
                if (r.simFailed.isNotEmpty()) {
                    ResultLine("SIM: chưa lưu được", "${r.simFailed.size} số", C.Orange)
                    Text(r.simFailed.take(10).joinToString("\n"), color = C.Sub2, fontSize = 12.sp)
                    if (r.simFailed.size > 10) Text("… và ${r.simFailed.size - 10} số khác (đã có đủ bên Gmail)", color = C.Sub, fontSize = 12.sp)
                }
            }
        }
    }
}

/** Tab "File VCF": xuất / nhập danh bạ dạng chuẩn (không mã hóa). */
@Composable
private fun VcfTab(data: AppData, onChanged: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val account = data.selectedAccount
    var toGmail by remember { mutableStateOf(account != null) }
    var toSim by remember { mutableStateOf(false) }
    var fileUri by remember { mutableStateOf<Uri?>(null) }
    var fileLabel by remember { mutableStateOf("") }
    var found by remember { mutableStateOf<List<BackupContact>?>(null) }
    var busy by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    var error by remember { mutableStateOf<String?>(null) }
    var result by remember { mutableStateOf<RestoreResult?>(null) }
    val exportList = remember(data.contacts) { data.backup.collect(data.contacts) }

    val saver = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/x-vcard")) { uri ->
        if (uri != null) scope.launch {
            val ok = withContext(Dispatchers.IO) {
                try {
                    ctx.contentResolver.openOutputStream(uri)?.use { it.write(Vcf.write(exportList)) } != null
                } catch (e: Exception) { false }
            }
            Actions.toast(ctx, if (ok) "Đã xuất ${exportList.size} liên hệ ra file VCF" else "Không ghi được file")
        }
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            fileUri = uri; fileLabel = describeFile(ctx, uri); result = null; error = null; found = null
            scope.launch {
                found = withContext(Dispatchers.IO) {
                    try {
                        val bytes = ctx.contentResolver.openInputStream(uri)?.use { s ->
                            val b = s.readBytes()
                            if (b.size > 10 * 1024 * 1024) null else b
                        }
                        bytes?.let { Vcf.read(it) }
                    } catch (e: Exception) { null }
                }
                if (found == null) error = "Không đọc được file (file quá lớn hoặc không phải file VCF)."
                else if (found!!.isEmpty()) error = "File không có liên hệ nào có số điện thoại."
            }
        }
    }

    // Xuất
    CardBox {
        Text("📤  Xuất file VCF", color = C.Text, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        Gap(6)
        Text("File danh bạ chuẩn, mở được trên iPhone, máy khác, Google Danh bạ.", color = C.Sub2, fontSize = 14.sp)
        Gap(10)
        PrimaryButton("Xuất ${exportList.size} liên hệ", enabled = exportList.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
            saver.launch("danhba_" + java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date()) + ".vcf")
        }
        Gap(10)
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(C.Orange.copy(alpha = 0.15f))
                .border(1.dp, C.Orange, RoundedCornerShape(12.dp)).padding(10.dp)
        ) {
            Text(
                "⚠️ File VCF KHÔNG mã hóa, ai có file cũng đọc được. Để sao lưu an toàn hãy dùng tab \"Sao lưu\".",
                color = C.Orange, fontSize = 13.sp
            )
        }
    }

    // Nhập
    CardBox {
        Text("📥  Nhập file VCF", color = C.Text, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        Gap(6)
        Text("Chọn file .vcf → chọn lưu vào Gmail / SIM → số đã có sẽ tự bỏ qua.", color = C.Sub2, fontSize = 14.sp)
        Gap(10)
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(C.Bg).clickable { picker.launch(arrayOf("text/x-vcard", "text/vcard", "text/directory", "*/*")) }.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Description, null, tint = C.Blue)
            Spacer(Modifier.width(12.dp))
            Text(if (fileUri == null) "Chọn file VCF (.vcf)" else fileLabel, color = C.Text, fontSize = 14.sp, modifier = Modifier.weight(1f))
            Text(if (fileUri == null) "Chọn" else "Đổi file", color = C.Blue, fontSize = 13.sp)
        }
        found?.takeIf { it.isNotEmpty() }?.let { list ->
            Gap(8)
            Text("Tìm thấy ${list.size} liên hệ trong file", color = C.Green, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Gap(10)
            SelectCard(
                selected = toGmail, enabled = account != null,
                title = "Gmail", subtitle = account ?: "Chưa có Gmail trên máy",
                icon = Icons.Filled.Email, onClick = { toGmail = !toGmail }
            )
            Gap(8)
            SelectCard(
                selected = toSim, enabled = data.simInfo.available,
                title = "SIM 1",
                subtitle = if (!data.simInfo.available) "Không đọc được SIM"
                else if (data.simInfo.maxRecords > 0) "Còn trống ${data.simInfo.free} / ${data.simInfo.maxRecords} chỗ" else "SIM đang dùng",
                icon = Icons.Filled.SimCard, onClick = { toSim = !toSim }
            )
            Gap(10)
            PrimaryButton("Nhập ${list.size} liên hệ", enabled = !busy && (toGmail || toSim), modifier = Modifier.fillMaxWidth()) {
                busy = true; progress = 0f; result = null
                val acc = if (toGmail) account else null
                val sim = toSim
                scope.launch {
                    result = withContext(Dispatchers.IO) { data.backup.restore(list, acc, sim) { i, n -> progress = i.toFloat() / n } }
                    busy = false
                    onChanged()
                }
            }
        }
        if (busy) { Gap(10); LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth()) }
        error?.let { Gap(8); Text(it, color = C.Red, fontSize = 13.sp) }
    }

    result?.let { r ->
        CardBox(bg = C.GreenBg) {
            Text("✔ Nhập xong (${r.total} liên hệ trong file)", color = C.Green, fontWeight = FontWeight.Bold)
            Gap(8)
            if (toGmail && account != null) {
                ResultLine("Gmail: lưu mới", "${r.googleAdded} số")
                ResultLine("Gmail: đã có sẵn, bỏ qua", "${r.googleSkipped} số")
            }
            if (toSim) {
                ResultLine("SIM: lưu mới", "${r.simAdded} số")
                ResultLine("SIM: đã có sẵn, bỏ qua", "${r.simSkipped} số")
                if (r.simFailed.isNotEmpty()) ResultLine("SIM: chưa lưu được", "${r.simFailed.size} số", C.Orange)
            }
        }
    }
}

@Composable
private fun ResultLine(label: String, value: String, color: Color = C.Text) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, color = color, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text(value, color = color, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

private fun describeFile(ctx: Context, uri: Uri): String = try {
    ctx.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { c ->
        if (c.moveToFirst()) {
            val name = c.getString(0) ?: "file"
            val size = c.getLong(1)
            "$name · ${(size + 1023) / 1024} KB"
        } else null
    } ?: "Đã chọn file"
} catch (e: Exception) {
    "Đã chọn file"
}
