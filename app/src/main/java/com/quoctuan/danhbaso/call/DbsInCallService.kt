package com.quoctuan.danhbaso.call

import android.content.Intent
import android.os.PowerManager
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import androidx.core.app.NotificationManagerCompat

/**
 * Hệ thống gọi vào đây mỗi khi có cuộc gọi (chỉ khi Danh bạ số là ứng dụng Điện thoại mặc định).
 * Không tự phát nhạc chuông: hệ thống vẫn đổ chuông / rung như bình thường.
 */
class DbsInCallService : InCallService() {

    private val callback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            if (state == Call.STATE_ACTIVE && CallManager.speakerOnAnswer == call) {
                CallManager.speakerOnAnswer = null
                CallManager.setSpeaker(true)
            }
            GroupCall.onStateChanged(call, state)
            CallManager.changed()
            CallNotifier.update(this@DbsInCallService)
        }

        override fun onDetailsChanged(call: Call, details: Call.Details) {
            CallManager.changed()
        }
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        CallManager.service = this
        call.registerCallback(callback)
        CallManager.calls.add(call)
        GroupCall.onCallAdded(call)
        CallManager.changed()
        CallNotifier.update(this)

        val ringing = CallManager.state(call) == Call.STATE_RINGING
        val pm = getSystemService(PowerManager::class.java)
        val screenOn = pm?.isInteractive == true
        // Gọi đi: mở màn đang gọi ngay.
        // Gọi đến: máy khóa / tắt màn hình thì thông báo tự mở toàn màn hình;
        // đang dùng máy thì hiện thanh nhỏ phía trên (có nút Nghe / Loa / Từ chối).
        // Nếu thông báo bị tắt thì mở thẳng màn hình gọi để không lỡ cuộc gọi.
        if (!ringing || !screenOn || !NotificationManagerCompat.from(this).areNotificationsEnabled()) {
            openCallScreen()
        }
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        call.unregisterCallback(callback)
        CallManager.calls.remove(call)
        if (CallManager.speakerOnAnswer == call) CallManager.speakerOnAnswer = null
        GroupCall.onCallRemoved(call)
        // Gọi xong mà còn số ghi chưa lưu → nhắc
        if (CallManager.calls.isEmpty()) CallNote.remindIfPending(this)
        CallManager.changed()
        CallNotifier.update(this)
    }

    @Suppress("OVERRIDE_DEPRECATION", "DEPRECATION") // vẫn dùng được, bản thay thế chỉ có từ Android 14
    override fun onCallAudioStateChanged(audioState: CallAudioState?) {
        CallManager.audio = audioState
        CallManager.changed()
        CallNotifier.update(this)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        CallManager.service = null
        CallManager.calls.clear()
        CallManager.changed()
        CallNotifier.update(this)
        return super.onUnbind(intent)
    }

    private fun openCallScreen() {
        try {
            startActivity(
                Intent(this, CallActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (e: Exception) {
            // Không mở được (hiếm) – thông báo vẫn còn để bấm vào
        }
    }
}
