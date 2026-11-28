package com.quoctuan.danhbaso.call

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.telephony.TelephonyManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.quoctuan.danhbaso.MainActivity
import com.quoctuan.danhbaso.data.ContactsRepo
import com.quoctuan.danhbaso.data.Prefs
import com.quoctuan.danhbaso.data.SimRepo
import com.quoctuan.danhbaso.data.SimWriteResult

/** Ghi số khi đang gọi: lưu vào Gmail / SIM, nhắc lại nếu gõ số mà chưa lưu. */
object CallNote {
    /** Số đã gõ trong lúc gọi nhưng chưa bấm Lưu (gọi xong sẽ nhắc). */
    @Volatile var pending: String? = null

    private const val CH = "call_note"
    private const val NOTIF_ID = 4343

    fun hasSim(ctx: Context): Boolean = try {
        ctx.getSystemService(TelephonyManager::class.java)?.simState == TelephonyManager.SIM_STATE_READY
    } catch (e: Exception) {
        false
    }

    /** Lưu số. Trả về câu báo kết quả (chạy trong Dispatchers.IO). */
    fun save(ctx: Context, name: String, number: String, gmail: Boolean, sim: Boolean): String {
        val n = name.ifBlank { number }
        val done = ArrayList<String>()
        val fail = ArrayList<String>()
        if (gmail) {
            val repo = ContactsRepo(ctx)
            val accs = repo.googleAccounts()
            val acc = Prefs(ctx).account?.takeIf { it in accs } ?: accs.firstOrNull()
            when {
                acc == null -> fail += "máy chưa có Gmail"
                repo.insertGoogle(acc, n, listOf(number), false) -> done += "Gmail"
                else -> fail += "Gmail lỗi"
            }
        }
        if (sim) {
            when (SimRepo(ctx).insert(n, number)) {
                SimWriteResult.OK -> done += "SIM"
                SimWriteResult.SHORTENED -> done += "SIM (tên rút gọn)"
                SimWriteResult.FULL -> fail += "SIM đầy"
                SimWriteResult.NO_SIM -> fail += "không thấy SIM"
                SimWriteResult.ERROR -> fail += "máy không cho ghi SIM"
            }
        }
        if (done.isNotEmpty()) pending = null
        return when {
            done.isEmpty() -> "Chưa lưu được: " + fail.joinToString(", ")
            fail.isEmpty() -> "Đã lưu \"$n\" · ${done.joinToString(" + ")}"
            else -> "Đã lưu \"$n\" · ${done.joinToString(" + ")} (${fail.joinToString(", ")})"
        }
    }

    /** Danh bạ để xem trong màn gọi: (tên, số), xếp theo tên. */
    fun contacts(ctx: Context): List<Pair<String, String>> = try {
        val out = LinkedHashMap<String, Pair<String, String>>()
        ctx.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER),
            null, null, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " COLLATE LOCALIZED ASC"
        )?.use { c ->
            while (c.moveToNext()) {
                val name = c.getString(0) ?: continue
                val num = c.getString(1) ?: continue
                val k = name + "|" + com.quoctuan.danhbaso.util.PhoneUtil.key(num)
                if (k !in out) out[k] = name to num
            }
        }
        out.values.toList()
    } catch (e: Exception) {
        emptyList()
    }

    /** Gọi xong mà còn số chưa lưu → thông báo "Bấm để lưu". */
    fun remindIfPending(ctx: Context) {
        val num = pending ?: return
        pending = null
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = ctx.getSystemService(NotificationManager::class.java)
            if (nm?.getNotificationChannel(CH) == null) {
                nm?.createNotificationChannel(NotificationChannel(CH, "Số ghi lúc gọi", NotificationManager.IMPORTANCE_DEFAULT))
            }
        }
        val open = PendingIntent.getActivity(
            ctx, 7, Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", num, null)).setClass(ctx, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val n = NotificationCompat.Builder(ctx, CH)
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .setContentTitle("Số bạn ghi lúc gọi chưa lưu")
            .setContentText("$num · Bấm để lưu")
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        try {
            val allowed = Build.VERSION.SDK_INT < 33 ||
                androidx.core.content.ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.POST_NOTIFICATIONS) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
            if (allowed) NotificationManagerCompat.from(ctx).notify(NOTIF_ID, n)
        } catch (e: SecurityException) {
            // chưa cho phép thông báo
        }
    }
}
