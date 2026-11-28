package com.quoctuan.danhbaso.call

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telecom.Call
import android.telecom.TelecomManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.quoctuan.danhbaso.util.PhoneUtil

/**
 * Gọi nhóm: gọi lần lượt từng người, ai nghe máy thì tự gộp vào cuộc gọi hội nghị.
 * Không nghe sau [RING_TIMEOUT_MS] thì bỏ qua, gọi người kế. Cần nhà mạng hỗ trợ gọi hội nghị
 * và Danh bạ số phải là ứng dụng Điện thoại mặc định.
 */
object GroupCall {
    const val MAX_PEOPLE = 5
    private const val RING_TIMEOUT_MS = 30_000L

    enum class St { WAIT, RINGING, JOINED, MISSED, LEFT }

    class Member(val name: String, val number: String) {
        var status by mutableStateOf(St.WAIT)
        internal var call: Call? = null
        val key: String = PhoneUtil.key(number)
    }

    /** Đang trong một cuộc gọi nhóm. */
    var active by mutableStateOf(false)
        private set
    var title by mutableStateOf("Gọi nhóm")
        private set
    val members = mutableStateListOf<Member>()

    private var appCtx: Context? = null
    private var dialing: Member? = null
    private var lastMerge = 0L
    private val handler = Handler(Looper.getMainLooper())
    private val timeout = Runnable {
        dialing?.let { m ->
            m.call?.disconnect()
            if (m.status == St.RINGING || m.status == St.WAIT) m.status = St.MISSED
            dialing = null
            next()
        }
    }

    /** Lý do không gọi được (null = bắt đầu được). */
    fun cannotStart(ctx: Context): String? = when {
        !CallRole.isDefault(ctx) -> "Gọi nhóm cần bật ☰ → Màn hình gọi → Của Danh bạ số"
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED ->
            "Chưa cho phép app gọi điện"
        CallManager.calls.isNotEmpty() -> "Đang có cuộc gọi, kết thúc rồi hãy gọi nhóm"
        else -> null
    }

    /** Bắt đầu gọi nhóm với danh sách (tên, số). */
    fun start(ctx: Context, groupTitle: String?, people: List<Pair<String, String>>): String? {
        cannotStart(ctx)?.let { return it }
        val list = people.filter { it.second.isNotBlank() }.distinctBy { PhoneUtil.key(it.second) }.take(MAX_PEOPLE)
        if (list.isEmpty()) return "Chưa chọn ai có số điện thoại"
        appCtx = ctx.applicationContext
        members.clear()
        members.addAll(list.map { Member(it.first, it.second) })
        title = groupTitle?.takeIf { it.isNotBlank() } ?: "Gọi nhóm"
        active = true
        dialing = null
        next()
        return null
    }

    /** Gọi lại một người không nghe máy / đã rời. */
    fun retry(m: Member) {
        if (!active) return
        m.status = St.WAIT
        m.call = null
        if (dialing == null) next()
    }

    /** Cho một người rời nhóm. */
    fun kick(m: Member) {
        m.call?.disconnect()
        m.status = St.LEFT
    }

    /** Kết thúc cả nhóm / hủy đang mời. */
    fun endAll() {
        handler.removeCallbacks(timeout)
        val calls = CallManager.calls.toList()
        finish()
        calls.forEach { runCatching { it.disconnect() } }
    }

    private fun finish() {
        handler.removeCallbacks(timeout)
        active = false
        dialing = null
        members.clear()
    }

    /** Gọi người kế tiếp đang chờ. */
    private fun next() {
        if (!active) return
        val ctx = appCtx ?: return
        val m = members.firstOrNull { it.status == St.WAIT } ?: run {
            // Không còn ai chờ: nếu không ai vào được thì kết thúc
            if (members.none { it.status == St.JOINED } && CallManager.calls.isEmpty()) finish()
            return
        }
        dialing = m
        m.status = St.RINGING
        try {
            if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                ctx.getSystemService(TelecomManager::class.java)
                    ?.placeCall(Uri.fromParts("tel", m.number, null), Bundle())
            }
        } catch (e: Exception) {
            m.status = St.MISSED
            dialing = null
            handler.post { next() }
            return
        }
        handler.removeCallbacks(timeout)
        handler.postDelayed(timeout, RING_TIMEOUT_MS)
    }

    // ───── Gọi từ DbsInCallService ─────

    internal fun onCallAdded(call: Call) {
        if (!active) return
        val k = PhoneUtil.key(CallManager.number(call))
        val m = dialing?.takeIf { it.key == k } ?: members.firstOrNull { it.key == k && it.call == null } ?: return
        m.call = call
    }

    internal fun onStateChanged(call: Call, state: Int) {
        if (!active) return
        val m = members.firstOrNull { it.call == call } ?: return
        when (state) {
            Call.STATE_ACTIVE -> if (m.status != St.JOINED) {
                m.status = St.JOINED
                if (dialing == m) {
                    handler.removeCallbacks(timeout)
                    dialing = null
                }
                mergeInto(call)
                // Chờ gộp xong rồi gọi người kế
                handler.postDelayed({ next() }, 1500)
            }
            Call.STATE_DISCONNECTED -> onGone(m)
        }
    }

    internal fun onCallRemoved(call: Call) {
        if (!active) return
        members.firstOrNull { it.call == call }?.let { onGone(it) }
        // Cuộc gọi hội nghị bị tắt hết (bạn tắt máy) → kết thúc nhóm
        if (CallManager.calls.isEmpty() && dialing == null) finish()
    }

    private fun onGone(m: Member) {
        // Một số nhà mạng gộp bằng cách tắt cuộc riêng rồi tạo cuộc hội nghị mới: vẫn tính là đang trong nhóm
        val conf = CallManager.calls.any { it.details.hasProperty(Call.Details.PROPERTY_CONFERENCE) }
        if (m.status == St.JOINED && conf && System.currentTimeMillis() - lastMerge < 8000) {
            m.call = null
            return
        }
        val wasDialing = dialing == m
        m.status = when (m.status) {
            St.JOINED -> St.LEFT
            St.LEFT -> St.LEFT
            else -> St.MISSED
        }
        m.call = null
        if (wasDialing) {
            handler.removeCallbacks(timeout)
            dialing = null
            handler.postDelayed({ next() }, 800)
        }
    }

    /** Gộp cuộc gọi vừa nghe máy vào cuộc đang giữ máy (hoặc vào cuộc gọi hội nghị). */
    private fun mergeInto(newCall: Call) {
        val others = CallManager.calls.filter { it != newCall && it.parent == null && CallManager.state(it) != Call.STATE_DISCONNECTED }
        val target = others.firstOrNull { CallManager.state(it) == Call.STATE_HOLDING } ?: others.firstOrNull() ?: return
        lastMerge = System.currentTimeMillis()
        try {
            when {
                newCall.details.can(Call.Details.CAPABILITY_MERGE_CONFERENCE) -> newCall.conference(target)
                target.details.can(Call.Details.CAPABILITY_MERGE_CONFERENCE) -> target.conference(newCall)
                else -> {
                    // Một số nhà mạng chỉ cho gộp qua cuộc gọi hội nghị đang có
                    newCall.conference(target)
                    target.mergeConference()
                }
            }
        } catch (e: Exception) {
            // Nhà mạng không cho gộp – vẫn giữ 2 cuộc, người dùng bấm "Đổi" để nói từng người
        }
    }

    /** Tên hiển thị cho một số (nếu có trong nhóm). */
    fun nameFor(number: String): String? {
        val k = PhoneUtil.key(number)
        return members.firstOrNull { it.key == k }?.name
    }
}
