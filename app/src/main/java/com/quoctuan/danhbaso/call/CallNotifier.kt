package com.quoctuan.danhbaso.call

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.telecom.Call
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/** Thông báo cuộc gọi: gọi đến (Từ chối · Nghe bằng loa · Nghe) và đang gọi (Loa · Kết thúc). */
object CallNotifier {
    private const val CH_IN = "calls_incoming"
    private const val CH_ON = "calls_ongoing"
    private const val ID = 4242

    const val ACT_DECLINE = "com.quoctuan.danhbaso.call.DECLINE"
    const val ACT_HANGUP = "com.quoctuan.danhbaso.call.HANGUP"
    const val ACT_SPEAKER = "com.quoctuan.danhbaso.call.SPEAKER"

    /** Extra cho CallActivity: "answer" hoặc "answer_speaker". */
    const val EXTRA_DO = "do"

    private fun channels(ctx: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val nm = ctx.getSystemService(NotificationManager::class.java) ?: return
        if (nm.getNotificationChannel(CH_IN) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CH_IN, "Cuộc gọi đến", NotificationManager.IMPORTANCE_HIGH).apply {
                    // Hệ thống đã đổ chuông, thông báo không kêu thêm
                    setSound(null, null)
                    enableVibration(false)
                }
            )
        }
        if (nm.getNotificationChannel(CH_ON) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CH_ON, "Đang gọi", NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    private fun activityPi(ctx: Context, req: Int, todo: String?): PendingIntent {
        val i = Intent(ctx, CallActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        if (todo != null) i.putExtra(EXTRA_DO, todo)
        return PendingIntent.getActivity(ctx, req, i, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun broadcastPi(ctx: Context, action: String): PendingIntent =
        PendingIntent.getBroadcast(
            ctx, action.hashCode(),
            Intent(ctx, CallActionReceiver::class.java).setAction(action),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

    fun update(ctx: Context) {
        val nm = NotificationManagerCompat.from(ctx)
        val c = CallManager.current()
        if (c == null) {
            nm.cancel(ID)
            return
        }
        channels(ctx)
        val ringing = CallManager.state(c) == Call.STATE_RINGING
        val name = CallInfo.display(ctx, CallManager.number(c))
        val b = NotificationCompat.Builder(ctx, if (ringing) CH_IN else CH_ON)
            .setSmallIcon(android.R.drawable.sym_action_call)
            .setContentTitle(name)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(activityPi(ctx, 1, null))
        if (ringing) {
            b.setContentText("Cuộc gọi đến…")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setFullScreenIntent(activityPi(ctx, 2, null), true)
                .addAction(0, "Từ chối", broadcastPi(ctx, ACT_DECLINE))
                .addAction(0, "🔊 Nghe bằng loa", activityPi(ctx, 3, "answer_speaker"))
                .addAction(0, "Nghe", activityPi(ctx, 4, "answer"))
        } else {
            val st = CallManager.state(c)
            val text = when (st) {
                Call.STATE_DIALING, Call.STATE_CONNECTING -> "Đang gọi…"
                Call.STATE_HOLDING -> "Đang giữ máy"
                else -> "Đang gọi"
            }
            b.setContentText(text).setOnlyAlertOnce(true).setPriority(NotificationCompat.PRIORITY_LOW)
            val since = c.details?.connectTimeMillis ?: 0L
            if (st == Call.STATE_ACTIVE && since > 0) b.setUsesChronometer(true).setWhen(since).setShowWhen(true)
            b.addAction(0, if (CallManager.isSpeaker()) "🔊 Tắt loa" else "🔊 Loa ngoài", broadcastPi(ctx, ACT_SPEAKER))
                .addAction(0, "Kết thúc", broadcastPi(ctx, ACT_HANGUP))
        }
        try {
            val allowed = Build.VERSION.SDK_INT < 33 ||
                ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            if (allowed && nm.areNotificationsEnabled()) nm.notify(ID, b.build())
        } catch (e: SecurityException) {
            // Chưa cho phép thông báo – màn hình gọi vẫn mở trực tiếp
        }
    }
}

/** Nhận các nút trên thông báo không cần mở màn hình: Từ chối, Kết thúc, bật/tắt loa. */
class CallActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            CallNotifier.ACT_DECLINE -> CallManager.ringing()?.let { CallManager.decline(it) }
            CallNotifier.ACT_HANGUP -> CallManager.current()?.let { CallManager.hangup(it) }
            CallNotifier.ACT_SPEAKER -> CallManager.setSpeaker(!CallManager.isSpeaker())
        }
    }
}
