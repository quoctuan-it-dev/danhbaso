package com.quoctuan.danhbaso.call

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.ContactsContract
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.TelecomManager
import android.telecom.VideoProfile
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Trạng thái các cuộc gọi đang diễn ra – chỉ dùng khi Danh bạ số là "ứng dụng Điện thoại" mặc định
 * (☰ → Màn hình gọi → Của Danh bạ số). Nhạc chuông, rung, ghi lịch sử cuộc gọi vẫn do hệ thống lo.
 */
object CallManager {
    /** Các cuộc gọi hiện có (thường 1, có thể 2 khi có cuộc gọi chờ). */
    val calls = mutableStateListOf<Call>()

    /** Tăng mỗi khi trạng thái cuộc gọi đổi, để giao diện vẽ lại. */
    var tick by mutableIntStateOf(0)

    /** Đường âm thanh hiện tại (loa trong / loa ngoài / tai nghe…). */
    var audio by mutableStateOf<CallAudioState?>(null)

    /** Dịch vụ cuộc gọi đang chạy (do hệ thống gắn vào). */
    internal var service: DbsInCallService? = null

    /** Cuộc gọi được bấm "Nghe bằng loa": bật loa ngay khi bắt máy xong. */
    internal var speakerOnAnswer: Call? = null

    fun state(c: Call): Int =
        if (Build.VERSION.SDK_INT >= 31) c.details.state else @Suppress("DEPRECATION") c.state

    fun number(c: Call): String = c.details?.handle?.schemeSpecificPart ?: ""

    /** Cuộc gọi đang đổ chuông (gọi đến). */
    fun ringing(): Call? = calls.firstOrNull { state(it) == Call.STATE_RINGING }

    /** Cuộc gọi chính để hiện lên màn hình. */
    fun current(): Call? {
        // Bỏ qua các cuộc "con" nằm trong cuộc gọi hội nghị
        val top = calls.filter { it.parent == null }
        return ringing()
            ?: top.firstOrNull { val s = state(it); s != Call.STATE_HOLDING && s != Call.STATE_DISCONNECTED }
            ?: top.firstOrNull { state(it) != Call.STATE_DISCONNECTED }
    }

    fun isSpeaker(): Boolean = audio?.route == CallAudioState.ROUTE_SPEAKER
    fun isMuted(): Boolean = audio?.isMuted == true

    @Suppress("DEPRECATION")
    fun setSpeaker(on: Boolean) {
        service?.setAudioRoute(if (on) CallAudioState.ROUTE_SPEAKER else CallAudioState.ROUTE_WIRED_OR_EARPIECE)
    }

    fun setMuted(m: Boolean) {
        service?.setMuted(m)
    }

    fun answer(c: Call, speaker: Boolean = false) {
        if (speaker) speakerOnAnswer = c
        c.answer(VideoProfile.STATE_AUDIO_ONLY)
    }

    fun decline(c: Call) = c.reject(false, null)

    fun declineWithText(c: Call, text: String) = c.reject(true, text)

    fun hangup(c: Call) = c.disconnect()

    /** Đổi qua lại giữa 2 cuộc gọi (1 đang nói, 1 đang giữ máy). */
    fun swap() {
        val active = calls.firstOrNull { state(it) == Call.STATE_ACTIVE }
        val held = calls.firstOrNull { state(it) == Call.STATE_HOLDING }
        if (held != null) {
            if (active != null) active.hold()
            held.unhold()
        }
    }

    internal fun changed() {
        tick++
    }
}

/** Tra tên trong danh bạ máy theo số (chạy nhanh, không cần mở app). */
object CallInfo {
    private val cache = HashMap<String, String?>()

    fun name(ctx: Context, number: String): String? {
        if (number.isBlank()) return null
        cache[number]?.let { return it }
        val n = try {
            val uri = android.net.Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI, android.net.Uri.encode(number)
            )
            ctx.contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME), null, null, null)
                ?.use { if (it.moveToFirst()) it.getString(0) else null }
        } catch (e: Exception) {
            null
        }
        if (n != null) cache[number] = n
        return n
    }

    /** Tên để hiện: tên trong danh bạ, không có thì hiện số, số ẩn thì "Số ẩn". */
    fun display(ctx: Context, number: String): String =
        name(ctx, number) ?: number.ifBlank { "Số ẩn" }

    /** Tên SIM nhận/gọi (VD "SIM 1 · Viettel"), null nếu không đọc được. */
    fun simLabel(ctx: Context, c: Call): String? = try {
        val tm = ctx.getSystemService(TelecomManager::class.java)
        val h = c.details?.accountHandle
        if (h == null || tm == null) null else tm.getPhoneAccount(h)?.label?.toString()
    } catch (e: Exception) {
        null
    }
}

/** Đặt / bỏ Danh bạ số làm "ứng dụng Điện thoại" mặc định. */
object CallRole {
    fun isDefault(ctx: Context): Boolean = try {
        ctx.getSystemService(TelecomManager::class.java)?.defaultDialerPackage == ctx.packageName
    } catch (e: Exception) {
        false
    }

    /** Hộp thoại của Android hỏi có đặt app làm ứng dụng Điện thoại không. */
    fun requestIntent(ctx: Context): Intent =
        if (Build.VERSION.SDK_INT >= 29) {
            ctx.getSystemService(RoleManager::class.java).createRequestRoleIntent(RoleManager.ROLE_DIALER)
        } else {
            Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER)
                .putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, ctx.packageName)
        }

    /** Trang Cài đặt → Ứng dụng mặc định, để đổi lại về app Điện thoại của máy. */
    fun settingsIntent(): Intent = Intent(android.provider.Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
}
