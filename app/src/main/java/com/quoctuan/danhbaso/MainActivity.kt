package com.quoctuan.danhbaso

import android.Manifest
import android.content.pm.PackageManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.offset
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import com.quoctuan.danhbaso.ui.glow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.quoctuan.danhbaso.data.AppData
import com.quoctuan.danhbaso.ui.AppInfoScreen
import com.quoctuan.danhbaso.ui.AppTheme
import com.quoctuan.danhbaso.ui.BackupScreen
import com.quoctuan.danhbaso.ui.C
import com.quoctuan.danhbaso.ui.ContactDetailScreen
import com.quoctuan.danhbaso.ui.ContactsScreen
import com.quoctuan.danhbaso.ui.DialpadScreen
import com.quoctuan.danhbaso.ui.EditContactScreen
import com.quoctuan.danhbaso.ui.Gap
import com.quoctuan.danhbaso.ui.NumberDetailScreen
import com.quoctuan.danhbaso.ui.PickExistingScreen
import com.quoctuan.danhbaso.ui.RecentsScreen
import com.quoctuan.danhbaso.ui.UpdateScreen
import com.quoctuan.danhbaso.util.Actions
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Số được app khác (hoặc màn hình gọi → "Thêm người") nhờ mở bằng bàn phím. "" = chỉ mở bàn phím. */
object DialRequest {
    var number by mutableStateOf<String?>(null)

    fun from(i: Intent?) {
        if (i?.action == Intent.ACTION_DIAL || (i?.action == Intent.ACTION_VIEW && i.data?.scheme == "tel")) {
            number = i.data?.schemeSpecificPart ?: ""
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        DialRequest.from(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) DialRequest.from(intent)
        // Che trắng nội dung app khi xem danh sách app đang mở (vẫn chụp màn hình được bình thường)
        if (Build.VERSION.SDK_INT >= 33) setRecentsScreenshotEnabled(false)
        enableEdgeToEdge()
        val data = AppData(applicationContext)
        setContent {
            AppTheme(data.theme, data.darkMode) {
                // Cỡ chữ toàn app (menu ☰ → Cỡ chữ): nhân thêm vào cỡ chữ của điện thoại
                val d = androidx.compose.ui.platform.LocalDensity.current
                androidx.compose.runtime.CompositionLocalProvider(
                    androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(d.density, d.fontScale * data.fontFactor)
                ) {
                    Box(Modifier.fillMaxSize().background(C.background).safeDrawingPadding()) {
                        // Kiểm tra bảo mật mỗi lần mở app, qua rồi mới vào app
                        var secured by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
                        if (secured) Root(data)
                        else com.quoctuan.danhbaso.ui.SecurityGate(data) { secured = true }
                    }
                }
            }
        }
    }
}

private val REQUIRED = arrayOf(Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS)
private val ALL = arrayOf(
    Manifest.permission.READ_CONTACTS,
    Manifest.permission.WRITE_CONTACTS,
    Manifest.permission.READ_CALL_LOG,
    Manifest.permission.CALL_PHONE,
    Manifest.permission.READ_PHONE_STATE,
    Manifest.permission.GET_ACCOUNTS,
)

/** Các màn hình phụ (mở chồng lên 3 tab chính). */
sealed interface Route {
    data object Tabs : Route
    data class Contact(val id: Long) : Route
    data class Number(val number: String) : Route
    data class Edit(val id: Long?, val prefill: String?) : Route
    data class PickExisting(val number: String) : Route
    data object Backup : Route
    data object Info : Route
    data object Update : Route
    data object Duplicates : Route
    data object CopySim : Route
    data object Security : Route
    data object CallScreen : Route
    data class GroupCallPick(val preselect: List<Long>, val title: String?) : Route
}

enum class Tab { RECENTS, DIALPAD, CONTACTS }

@Composable
private fun Root(data: AppData) {
    val ctx = LocalContext.current
    fun granted(p: String) = ContextCompat.checkSelfPermission(ctx, p) == PackageManager.PERMISSION_GRANTED
    var permTick by remember { mutableIntStateOf(0) }
    val hasRequired = remember(permTick) { REQUIRED.all { granted(it) } }
    val hasCallLog = remember(permTick) { granted(Manifest.permission.READ_CALL_LOG) }
    val scope = rememberCoroutineScope()

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        permTick++
        scope.launch { data.refresh() }
    }
    LaunchedEffect(Unit) { if (!ALL.all { granted(it) }) launcher.launch(ALL) }

    // Đọc lại dữ liệu mỗi lần quay lại app
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME) {
                permTick++
                scope.launch { if (REQUIRED.all { granted(it) }) data.refresh() }
            }
        }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }

    if (!hasRequired) {
        PermissionScreen(onGrant = { launcher.launch(ALL) }, onSettings = { Actions.openAppSettings(ctx) })
    } else {
        MainNav(data, hasCallLog, onGrant = { launcher.launch(ALL) })
    }
}

@Composable
private fun PermissionScreen(onGrant: () -> Unit, onSettings: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(28.dp),
        verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Danh bạ số cần quyền Danh bạ", color = C.Text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Gap(10)
        Text(
            "App đọc và lưu số vào danh bạ chung của điện thoại (dùng chung với app Điện thoại gốc). App không có quyền Internet nên không gửi dữ liệu ra ngoài.",
            color = C.Sub2, fontSize = 14.sp
        )
        Gap(20)
        Button(onClick = onGrant) { Text("Cấp quyền") }
        TextButton(onClick = onSettings) { Text("Mở Cài đặt ứng dụng") }
    }
}

@Composable
private fun MainNav(data: AppData, hasCallLog: Boolean, onGrant: () -> Unit) {
    val scope = rememberCoroutineScope()
    val stack = remember { mutableStateListOf<Route>(Route.Tabs) }
    var tab by remember { mutableStateOf(Tab.RECENTS) }
    var focusSearch by remember { mutableStateOf(false) }
    var showKeypadSize by remember { mutableStateOf(false) }
    var showTheme by remember { mutableStateOf(false) }
    var showFont by remember { mutableStateOf(false) }
    val drawer = rememberDrawerState(DrawerValue.Closed)

    fun push(r: Route) = stack.add(r)
    fun pop() { if (stack.size > 1) stack.removeAt(stack.lastIndex) }
    val refresh: () -> Unit = { scope.launch { data.refresh() } }

    BackHandler(enabled = stack.size > 1) { pop() }
    BackHandler(enabled = stack.size == 1 && drawer.isOpen) { scope.launch { drawer.close() } }
    BackHandler(enabled = stack.size == 1 && !drawer.isOpen && tab != Tab.RECENTS) { tab = Tab.RECENTS }

    fun openNumber(n: String) {
        val c = data.contactFor(n)
        push(if (c != null) Route.Contact(c.id) else Route.Number(n))
    }

    // App khác nhờ mở bàn phím / mở một số (khi Danh bạ số là ứng dụng Điện thoại)
    val dialReq = DialRequest.number
    LaunchedEffect(dialReq, data.loaded) {
        if (dialReq != null && data.loaded) {
            DialRequest.number = null
            while (stack.size > 1) stack.removeAt(stack.lastIndex)
            tab = Tab.DIALPAD
            if (dialReq.isNotBlank()) openNumber(dialReq)
        }
    }

    when (val r = stack.last()) {
        Route.Tabs -> ModalNavigationDrawer(
            drawerState = drawer,
            gesturesEnabled = tab == Tab.RECENTS,
            drawerContent = {
                AppDrawer(data, onKeypadSize = {
                    scope.launch { drawer.close() }
                    showKeypadSize = true
                }, onTheme = {
                    scope.launch { drawer.close() }
                    showTheme = true
                }, onFont = {
                    scope.launch { drawer.close() }
                    showFont = true
                }) { dest ->
                    // Giữ menu mở: bấm ← ở màn con sẽ quay về đúng menu này
                    push(dest)
                }
            }
        ) {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f)) {
                  // Chuyển tab: trang trượt ngang theo hướng tab + mờ dần (~0,3 giây)
                  AnimatedContent(
                    targetState = tab,
                    modifier = Modifier.fillMaxSize(),
                    transitionSpec = {
                        val dir = if (targetState.ordinal > initialState.ordinal) 1 else -1
                        (slideInHorizontally(tween(300, easing = FastOutSlowInEasing)) { w -> dir * w } +
                            fadeIn(tween(300))) togetherWith
                            (slideOutHorizontally(tween(300, easing = FastOutSlowInEasing)) { w -> -dir * w } +
                                fadeOut(tween(250))) using SizeTransform(clip = true)
                    },
                    label = "tab"
                  ) { t ->
                    when (t) {
                        Tab.RECENTS -> RecentsScreen(
                            data, hasCallLog,
                            onMenu = { scope.launch { drawer.open() } },
                            onSearch = { focusSearch = true; tab = Tab.CONTACTS },
                            onOpen = { openNumber(it) },
                            onBackup = { push(Route.Backup) },
                            onGrant = onGrant,
                        )
                        Tab.DIALPAD -> DialpadScreen(data, onOpenNumber = { openNumber(it) }, onSaveNumber = { push(Route.Edit(null, it)) })
                        Tab.CONTACTS -> ContactsScreen(
                            data, focusSearch,
                            onOpen = { c -> push(Route.Contact(c.id)) },
                            onAdd = { push(Route.Edit(null, null)) },
                            onChanged = refresh,
                            onGroupCall = { ids, title -> push(Route.GroupCallPick(ids, title)) },
                        )
                    }
                  }
                }
                BottomBar(tab, onSelect = { t -> if (t == Tab.CONTACTS) focusSearch = false; tab = t })
            }
        }

        is Route.Contact -> {
            val c = data.contactById(r.id)
            if (c == null) {
                LaunchedEffect(r) { pop() }
            } else {
                ContactDetailScreen(data, c, onBack = { pop() }, onEdit = { push(Route.Edit(c.id, null)) }, onChanged = refresh)
            }
        }

        is Route.Number -> NumberDetailScreen(
            data, r.number, onBack = { pop() },
            onSave = { push(Route.Edit(null, r.number)) },
            onAddToExisting = { push(Route.PickExisting(r.number)) },
        )

        is Route.Edit -> EditContactScreen(
            data,
            existing = r.id?.let { data.contactById(it) },
            prefillNumber = r.prefill,
            onClose = { pop() },
            onSaved = {
                scope.launch {
                    data.refresh()
                    pop()
                    // Lưu từ số lạ: đóng luôn màn số lạ để quay về danh sách
                    if (r.prefill != null && stack.lastOrNull() is Route.Number) pop()
                }
            },
            onPickAccount = {},
        )

        is Route.PickExisting -> PickExistingScreen(data, r.number, onBack = { pop() }, onDone = { id ->
            scope.launch {
                data.refresh()
                pop()
                if (stack.lastOrNull() is Route.Number) pop()
                push(Route.Contact(id))
            }
        })

        Route.Backup -> BackupScreen(data, onBack = { pop() }, onChanged = refresh)
        Route.Info -> AppInfoScreen(onBack = { pop() })
        Route.Update -> UpdateScreen(onBack = { pop() })
        Route.Duplicates -> com.quoctuan.danhbaso.ui.DuplicatesScreen(data, onBack = { pop() }, onChanged = refresh)
        Route.CopySim -> com.quoctuan.danhbaso.ui.CopySimGmailScreen(data, onBack = { pop() }, onChanged = refresh)
        Route.Security -> com.quoctuan.danhbaso.ui.SecurityScreen(data, onBack = { pop() })
        Route.CallScreen -> com.quoctuan.danhbaso.ui.CallScreenSettings(onBack = { pop() })
        is Route.GroupCallPick -> com.quoctuan.danhbaso.ui.GroupCallPickScreen(data, r.preselect, r.title, onBack = { pop() })
    }
    if (showKeypadSize) com.quoctuan.danhbaso.ui.KeypadSizeDialog(data) { showKeypadSize = false }
    if (showTheme) com.quoctuan.danhbaso.ui.ThemeDialog(data) { showTheme = false }
    if (showFont) com.quoctuan.danhbaso.ui.FontSizeDialog(data) { showFont = false }
}

@Composable
private fun AppDrawer(data: AppData, onKeypadSize: () -> Unit, onTheme: () -> Unit, onFont: () -> Unit, onPick: (Route) -> Unit) {
    val ctx = LocalContext.current
    // Trạng thái bảo mật cho nhãn "An toàn / Cần lưu ý"
    val sec by androidx.compose.runtime.produceState<com.quoctuan.danhbaso.data.SecReport?>(null) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            runCatching { com.quoctuan.danhbaso.data.SecurityCheck.run(ctx) }.getOrNull()
        }
    }
    val isAppCall = com.quoctuan.danhbaso.call.CallRole.isDefault(ctx)
    ModalDrawerSheet(
        drawerContainerColor = C.Bg, drawerShape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp),
        modifier = Modifier.width(318.dp)
    ) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 12.dp)) {
            DrawerHeader(data.contacts.size, data.lastBackup)
            DrawerSection("Dữ liệu") {
                val last = data.lastBackup
                DrawerRow(Icons.Filled.Shield, Color(0xFF2F86FF), "Sao lưu và khôi phục",
                    if (last == 0L) "Chưa" else SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date(last))) { onPick(Route.Backup) }
                DrawerRow(Icons.Filled.CleaningServices, Color(0xFFF2A33A), "Dọn danh bạ trùng") { onPick(Route.Duplicates) }
                DrawerRow(Icons.Filled.SwapHoriz, Color(0xFF8E6CF0), "Chép SIM ↔ Gmail") { onPick(Route.CopySim) }
            }
            DrawerSection("Bảo mật & cuộc gọi") {
                val r = sec
                val (secText, secColor) = when {
                    r == null -> "…" to C.Sub2
                    r.danger.isNotEmpty() -> "● Nguy hiểm" to C.Red
                    r.warnings.isNotEmpty() -> "● Cần lưu ý" to C.Orange
                    else -> "● An toàn" to C.Green
                }
                DrawerRow(Icons.Filled.VerifiedUser, Color(0xFF1FB57A), "Bảo mật", secText, secColor) { onPick(Route.Security) }
                DrawerRow(
                    Icons.Filled.PhoneInTalk, Color(0xFF1FB5E8), "Màn hình gọi", if (isAppCall) "Danh bạ số" else "Điện thoại",
                    chip = if (isAppCall) ChipStyle.NAVY_GLOW else ChipStyle.GREEN_GLOW
                ) { onPick(Route.CallScreen) }
            }
            DrawerSection("Giao diện") {
                DrawerRow(Icons.Filled.Dialpad, Color(0xFF14B8C4), "Cỡ bàn phím", listOf("Nhỏ", "Vừa", "Lớn")[data.keypadSize], onClick = onKeypadSize)
                DrawerRow(Icons.Filled.FormatSize, Color(0xFFFF8A3D), "Cỡ chữ", com.quoctuan.danhbaso.data.FONT_NAMES[data.fontSize], onClick = onFont)
                DrawerRow(Icons.Filled.Palette, Color(0xFFE0569A), "Màu giao diện", com.quoctuan.danhbaso.ui.THEME_NAMES[data.theme], dot = C.Blue, onClick = onTheme)
            }
            DrawerSection("Ứng dụng") {
                DrawerRow(Icons.Filled.Info, Color(0xFF5B6CFF), "Thông tin ứng dụng") { onPick(Route.Info) }
                DrawerRow(Icons.Filled.Refresh, Color(0xFF2F86FF), "Cập nhật ứng dụng", "v${BuildConfig.VERSION_NAME}") { onPick(Route.Update) }
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp)) {
                Text("Phát triển bởi Quốc Tuấn", color = C.Sub, fontSize = 12.sp, modifier = Modifier.weight(1f))
                Text("v${BuildConfig.VERSION_NAME}", color = C.Sub, fontSize = 12.sp)
            }
        }
    }
}

/** Thẻ đầu menu: logo, tên app, số liên hệ, ngày sao lưu. */
@Composable
private fun DrawerHeader(contacts: Int, lastBackup: Long) {
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
            .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFF0E3A6E), Color(0xFF1565C0), Color(0xFF1FB5E8))))
    ) {
        Box(
            Modifier.align(Alignment.TopEnd).offset(30.dp, (-30).dp).size(120.dp)
                .border(2.dp, Color.White.copy(alpha = 0.13f), CircleShape)
        )
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(50.dp).clip(RoundedCornerShape(15.dp)),
                contentAlignment = Alignment.Center
            ) {
                val m = Modifier.size(74.dp)
                androidx.compose.foundation.Image(androidx.compose.ui.res.painterResource(R.drawable.ic_launcher_background), null, m)
                androidx.compose.foundation.Image(androidx.compose.ui.res.painterResource(R.drawable.ic_launcher_foreground), null, m)
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Danh bạ số", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    HeaderTag("$contacts liên hệ")
                    val last = lastBackup
                    HeaderTag(if (last == 0L) "Chưa sao lưu" else "Sao lưu " + SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date(last)))
                }
            }
        }
    }
}

@Composable
private fun HeaderTag(text: String) {
    Text(
        text, color = Color.White, fontSize = 11.5.sp, maxLines = 1,
        modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = 0.18f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

/** Một nhóm mục trong menu: tiêu đề nhỏ + thẻ bo góc. */
@Composable
private fun DrawerSection(title: String, content: @Composable () -> Unit) {
    Text(
        title.uppercase(), color = C.Sub, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp,
        modifier = Modifier.padding(start = 10.dp, top = 14.dp, bottom = 6.dp)
    )
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(C.Card).padding(vertical = 2.dp)) {
        content()
    }
}

/** Kiểu nhãn giá trị bên phải dòng menu. */
private enum class ChipStyle { PLAIN, NAVY_GLOW, GREEN_GLOW }

/** Một dòng menu: ô biểu tượng màu · tên · nhãn giá trị · mũi tên. */
@Composable
private fun DrawerRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    title: String,
    value: String? = null,
    valueColor: Color? = null,
    dot: Color? = null,
    chip: ChipStyle = ChipStyle.PLAIN,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(34.dp).clip(RoundedCornerShape(11.dp)).background(tint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) { Icon(icon, null, tint = tint, modifier = Modifier.size(19.dp)) }
        Spacer(Modifier.width(12.dp))
        Text(title, color = C.Text, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1, modifier = Modifier.weight(1f))
        if (value != null) {
            val shape = RoundedCornerShape(10.dp)
            val chipMod = when (chip) {
                // Màn hình gọi = Danh bạ số: nền xanh navy, viền phát sáng xanh
                ChipStyle.NAVY_GLOW -> Modifier.glow(shape, 8.dp, Color(0xFF2F86FF))
                    .clip(shape).background(Color(0xFF123C74)).border(1.2.dp, Color(0xFF5CC2FF), shape)
                // Màn hình gọi = Điện thoại: viền phát sáng xanh lá
                ChipStyle.GREEN_GLOW -> Modifier.glow(shape, 8.dp, Color(0xFF1FB57A))
                    .clip(shape).background(Color(0xFF1FB57A).copy(alpha = 0.14f)).border(1.2.dp, Color(0xFF2FC46E), shape)
                ChipStyle.PLAIN -> Modifier.clip(shape).background((valueColor ?: C.Sub2).copy(alpha = 0.12f))
            }
            val textColor = when (chip) {
                ChipStyle.NAVY_GLOW -> Color.White
                ChipStyle.GREEN_GLOW -> Color(0xFF1E9E5E)
                ChipStyle.PLAIN -> valueColor ?: C.Sub2
            }
            Row(
                chipMod.padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (dot != null) {
                    Box(Modifier.size(9.dp).clip(CircleShape).background(dot))
                    Spacer(Modifier.width(4.dp))
                }
                Text(value, color = textColor, fontSize = 11.5.sp, fontWeight = if (chip == ChipStyle.PLAIN) FontWeight.Medium else FontWeight.Bold, maxLines = 1)
            }
        }
        Text("›", color = C.Sub, fontSize = 18.sp, modifier = Modifier.padding(start = 6.dp))
    }
}

/** Thanh menu dưới (to, dễ bấm). Mục đang chọn có viên nền màu cyan → xanh dương. */
@android.annotation.SuppressLint("UnusedBoxWithConstraintsScope") // có dùng maxWidth để đặt viên nền
@Composable
private fun BottomBar(tab: Tab, onSelect: (Tab) -> Unit) {
    val items = listOf(
        Triple(Tab.RECENTS, "Gần đây", Icons.Outlined.Schedule),
        Triple(Tab.DIALPAD, "Bàn phím", Icons.Filled.Dialpad),
        Triple(Tab.CONTACTS, "Danh bạ", Icons.Outlined.Person),
    )
    androidx.compose.foundation.layout.BoxWithConstraints(
        Modifier.fillMaxWidth().background(C.Nav)
            .drawBehind {
                // đường sáng mảnh phía trên thanh menu
                drawLine(
                    androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(C.AccentA.copy(alpha = 0f), C.AccentA.copy(alpha = 0.7f), C.AccentB.copy(alpha = 0f))),
                    androidx.compose.ui.geometry.Offset(0f, 0f), androidx.compose.ui.geometry.Offset(size.width, 0f), strokeWidth = 2f
                )
            }
            .padding(top = 10.dp, bottom = 10.dp)
    ) {
        // Viên nền màu trượt theo tab đang chọn
        val cell = maxWidth / items.size
        val idx = items.indexOfFirst { it.first == tab }.coerceAtLeast(0)
        val pillX by animateDpAsState(
            cell * idx + (cell - 76.dp) / 2, tween(300, easing = FastOutSlowInEasing), label = "pill"
        )
        Box(
            Modifier.offset(x = pillX, y = 4.dp).width(76.dp).height(40.dp)
                .glow(RoundedCornerShape(20.dp), 12.dp)
                .clip(RoundedCornerShape(20.dp)).background(C.accent)
        )
        Row(Modifier.fillMaxWidth()) {
            for ((t, label, icon) in items) {
                val sel = t == tab
                val tint by animateColorAsState(if (sel) C.OnBlue else C.Sub2, tween(300), label = "tint")
                Column(
                    Modifier.weight(1f).clip(RoundedCornerShape(16.dp))
                        .clickable { onSelect(t) }.padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(Modifier.width(76.dp).height(40.dp), contentAlignment = Alignment.Center) {
                        Icon(icon, label, tint = tint, modifier = Modifier.size(28.dp))
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(label, color = if (sel) C.Text else C.Sub2, fontSize = 15.sp,
                        fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }
    }
}

// ───────────────────────── Xem trước menu ☰ trong Android Studio (Preview) ─────────────────────────
// Bấm nút "Split" góc trên bên phải để xem. Dữ liệu là DỮ LIỆU MẪU, không có trong app thật.

@Composable
private fun DrawerPreviewBody(mode: Int) {
    com.quoctuan.danhbaso.ui.AppTheme(theme = 0, mode = mode) {
        Column(Modifier.width(318.dp).background(C.Bg).padding(12.dp)) {
            DrawerHeader(44, System.currentTimeMillis())
            DrawerSection("Dữ liệu") {
                DrawerRow(Icons.Filled.Shield, Color(0xFF2F86FF), "Sao lưu và khôi phục", "26/11") {}
                DrawerRow(Icons.Filled.CleaningServices, Color(0xFFF2A33A), "Dọn danh bạ trùng") {}
                DrawerRow(Icons.Filled.SwapHoriz, Color(0xFF8E6CF0), "Chép SIM ↔ Gmail") {}
            }
            DrawerSection("Bảo mật & cuộc gọi") {
                DrawerRow(Icons.Filled.VerifiedUser, Color(0xFF1FB57A), "Bảo mật", "● An toàn", C.Green) {}
                DrawerRow(Icons.Filled.PhoneInTalk, Color(0xFF1FB5E8), "Màn hình gọi", "Danh bạ số", chip = ChipStyle.NAVY_GLOW) {}
            }
            DrawerSection("Giao diện") {
                DrawerRow(Icons.Filled.Dialpad, Color(0xFF14B8C4), "Cỡ bàn phím", "Vừa") {}
                DrawerRow(Icons.Filled.FormatSize, Color(0xFFFF8A3D), "Cỡ chữ", "Vừa") {}
                DrawerRow(Icons.Filled.Palette, Color(0xFFE0569A), "Màu giao diện", "Navy biển", dot = C.Blue) {}
            }
            DrawerSection("Ứng dụng") {
                DrawerRow(Icons.Filled.Info, Color(0xFF5B6CFF), "Thông tin ứng dụng") {}
                DrawerRow(Icons.Filled.Refresh, Color(0xFF2F86FF), "Cập nhật ứng dụng", "v1.4.3") {}
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "Menu · Sáng", heightDp = 820)
@Composable
private fun PreviewDrawerLight() = DrawerPreviewBody(mode = 1)

@androidx.compose.ui.tooling.preview.Preview(name = "Menu · Tối", heightDp = 820)
@Composable
private fun PreviewDrawerDark() = DrawerPreviewBody(mode = 2)
