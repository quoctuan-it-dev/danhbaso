package com.quoctuan.danhbaso

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import com.quoctuan.danhbaso.ui.glow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val data = AppData(applicationContext)
        setContent {
            AppTheme(data.theme) {
                // Cỡ chữ toàn app (menu ☰ → Cỡ chữ): nhân thêm vào cỡ chữ của điện thoại
                val d = androidx.compose.ui.platform.LocalDensity.current
                androidx.compose.runtime.CompositionLocalProvider(
                    androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(d.density, d.fontScale * data.fontFactor)
                ) {
                    Box(Modifier.fillMaxSize().background(C.background).safeDrawingPadding()) {
                        Root(data)
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
                    scope.launch { drawer.close() }
                    push(dest)
                }
            }
        ) {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f)) {
                    when (tab) {
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
                        )
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
    }
    if (showKeypadSize) com.quoctuan.danhbaso.ui.KeypadSizeDialog(data) { showKeypadSize = false }
    if (showTheme) com.quoctuan.danhbaso.ui.ThemeDialog(data) { showTheme = false }
    if (showFont) com.quoctuan.danhbaso.ui.FontSizeDialog(data) { showFont = false }
}

@Composable
private fun AppDrawer(data: AppData, onKeypadSize: () -> Unit, onTheme: () -> Unit, onFont: () -> Unit, onPick: (Route) -> Unit) {
    ModalDrawerSheet(drawerContainerColor = C.Nav, drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)) {
        Column(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 24.dp)) {
            Text("Danh bạ số", color = C.Text, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp))
            val last = data.lastBackup
            Text(
                "${data.contacts.size} liên hệ · " + if (last == 0L) "chưa sao lưu"
                else "sao lưu lần cuối " + SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date(last)),
                color = C.Sub, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
            Gap(16)
            val colors = NavigationDrawerItemDefaults.colors(
                unselectedContainerColor = C.Nav, unselectedTextColor = C.Text, unselectedIconColor = C.Sub2,
                selectedContainerColor = C.BlueBg, selectedTextColor = C.Text, selectedIconColor = C.Blue,
            )
            NavigationDrawerItem(
                label = { Text("Sao lưu và khôi phục", fontWeight = FontWeight.SemiBold) },
                icon = { Icon(Icons.Filled.Shield, null) }, selected = true, onClick = { onPick(Route.Backup) }, colors = colors
            )
            NavigationDrawerItem(
                label = { Text("Dọn danh bạ trùng") }, icon = { Icon(Icons.Filled.CleaningServices, null) },
                selected = false, onClick = { onPick(Route.Duplicates) }, colors = colors
            )
            NavigationDrawerItem(
                label = { Text("Chép SIM ↔ Gmail") }, icon = { Icon(Icons.Filled.SwapHoriz, null) },
                selected = false, onClick = { onPick(Route.CopySim) }, colors = colors
            )
            NavigationDrawerItem(
                label = { Text("Cỡ bàn phím: " + listOf("Nhỏ", "Vừa", "Lớn")[data.keypadSize]) },
                icon = { Icon(Icons.Filled.Dialpad, null) },
                selected = false, onClick = onKeypadSize, colors = colors
            )
            NavigationDrawerItem(
                label = { Text("Cỡ chữ: " + com.quoctuan.danhbaso.data.FONT_NAMES[data.fontSize]) },
                icon = { Icon(Icons.Filled.FormatSize, null) },
                selected = false, onClick = onFont, colors = colors
            )
            NavigationDrawerItem(
                label = { Text("Màu giao diện: " + com.quoctuan.danhbaso.ui.THEME_NAMES[data.theme]) },
                icon = { Icon(Icons.Filled.Palette, null) },
                selected = false, onClick = onTheme, colors = colors
            )
            NavigationDrawerItem(
                label = { Text("Thông tin ứng dụng") }, icon = { Icon(Icons.Filled.Info, null) },
                selected = false, onClick = { onPick(Route.Info) }, colors = colors
            )
            NavigationDrawerItem(
                label = { Text("Cập nhật ứng dụng") }, icon = { Icon(Icons.Filled.Refresh, null) },
                selected = false, onClick = { onPick(Route.Update) }, colors = colors
            )
            Spacer(Modifier.weight(1f))
            Text(
                "Danh bạ số được phát triển bởi Quốc Tuấn", color = C.Sub2, fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 14.dp)
            )
            Text("Phiên bản ${BuildConfig.VERSION_NAME}", color = C.Sub, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp))
        }
    }
}

/** Thanh menu dưới (to, dễ bấm). Mục đang chọn có viên nền màu cyan → xanh dương. */
@Composable
private fun BottomBar(tab: Tab, onSelect: (Tab) -> Unit) {
    val items = listOf(
        Triple(Tab.RECENTS, "Gần đây", Icons.Outlined.Schedule),
        Triple(Tab.DIALPAD, "Bàn phím", Icons.Filled.Dialpad),
        Triple(Tab.CONTACTS, "Danh bạ", Icons.Outlined.Person),
    )
    androidx.compose.foundation.layout.Row(
        Modifier.fillMaxWidth().background(C.Nav)
            .drawBehind {
                // đường sáng mảnh phía trên thanh menu
                drawLine(
                    androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(C.AccentA.copy(alpha = 0f), C.AccentA.copy(alpha = 0.7f), C.AccentB.copy(alpha = 0f))),
                    androidx.compose.ui.geometry.Offset(0f, 0f), androidx.compose.ui.geometry.Offset(size.width, 0f), strokeWidth = 2f
                )
            }
            .padding(top = 10.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        for ((t, label, icon) in items) {
            val sel = t == tab
            Column(
                Modifier.weight(1f).clip(RoundedCornerShape(16.dp))
                    .clickable { onSelect(t) }.padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier.width(76.dp).height(40.dp)
                        .then(if (sel) Modifier.glow(RoundedCornerShape(20.dp), 12.dp) else Modifier)
                        .clip(RoundedCornerShape(20.dp))
                        .then(if (sel) Modifier.background(C.accent) else Modifier),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, label, tint = if (sel) C.OnBlue else C.Sub2, modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.height(6.dp))
                Text(label, color = if (sel) C.Text else C.Sub2, fontSize = 15.sp,
                    fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
}
