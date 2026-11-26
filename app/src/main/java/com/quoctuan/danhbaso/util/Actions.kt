package com.quoctuan.danhbaso.util

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.telecom.TelecomManager
import android.widget.Toast
import androidx.core.content.ContextCompat

/** Đường dẫn trang Releases trên GitHub của app. */
const val GITHUB_RELEASES_URL = "https://github.com/quoctuan-it-dev/danhbaso/releases"

/** Các thao tác mở app khác: gọi, nhắn tin, Zalo, chặn số... */
object Actions {

    fun toast(ctx: Context, msg: String) = Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()

    private fun start(ctx: Context, intent: Intent, failMsg: String): Boolean = try {
        ctx.startActivity(intent)
        true
    } catch (e: Exception) {
        toast(ctx, failMsg)
        false
    }

    /** Gọi điện bằng màn hình gọi của hệ thống. */
    fun call(ctx: Context, number: String) {
        if (number.isBlank()) return
        val uri = Uri.fromParts("tel", number, null)
        val canCall = ContextCompat.checkSelfPermission(ctx, Manifest.permission.CALL_PHONE) ==
            PackageManager.PERMISSION_GRANTED
        val action = if (canCall) Intent.ACTION_CALL else Intent.ACTION_DIAL
        start(ctx, Intent(action, uri), "Không gọi được số này")
    }

    /** Sao chép số điện thoại vào bộ nhớ tạm để dán sang Zalo, tin nhắn... */
    fun copy(ctx: Context, number: String) {
        val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        cm.setPrimaryClip(ClipData.newPlainText("Số điện thoại", PhoneUtil.local(number)))
        // Android 13+ tự hiện thông báo "Đã sao chép", máy cũ thì app tự báo
        if (android.os.Build.VERSION.SDK_INT < 33) toast(ctx, "Đã sao chép ${PhoneUtil.pretty(number)}")
    }

    /** Gửi danh thiếp: tên + các số dạng chữ, chọn Zalo / Tin nhắn / Messenger... để gửi. */
    fun shareContact(ctx: Context, name: String, phones: List<String>) {
        val text = buildString {
            append("📇 ").append(name)
            for (p in phones) append("\n📞 ").append(PhoneUtil.pretty(p))
        }
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        start(ctx, Intent.createChooser(send, "Gửi danh thiếp qua…"), "Không mở được bảng chia sẻ")
    }

    fun sms(ctx: Context, number: String) {
        start(ctx, Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$number")), "Không mở được app tin nhắn")
    }

    /** Mở Zalo bằng đường link chính thức zalo.me/[số]. Số chưa có Zalo thì Zalo tự báo. */
    fun zalo(ctx: Context, number: String) {
        val uri = Uri.parse("https://zalo.me/${PhoneUtil.local(number)}")
        start(ctx, Intent(Intent.ACTION_VIEW, uri), "Không mở được Zalo")
    }

    /**
     * Chặn số bằng danh sách chặn CỦA ĐIỆN THOẠI (dùng chung với app Điện thoại gốc).
     * App chép sẵn số vào bộ nhớ tạm để bạn dán vào ô thêm số chặn.
     */
    fun block(ctx: Context, number: String) {
        val cm = ctx.getSystemService(ClipboardManager::class.java)
        cm?.setPrimaryClip(ClipData.newPlainText("so_dien_thoai", PhoneUtil.local(number)))
        val tm = ctx.getSystemService(TelecomManager::class.java)
        val opened = try {
            val intent = tm?.createManageBlockedNumbersIntent()
            if (intent != null) {
                ctx.startActivity(intent)
                true
            } else false
        } catch (e: Exception) {
            false
        }
        if (opened) {
            toast(ctx, "Đã chép số. Bấm Thêm số rồi dán vào để chặn.")
        } else {
            toast(ctx, "Đã chép số. Hãy chặn trong app Điện thoại: bấm vào số → Chặn.")
            start(ctx, Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", number, null)), "Không mở được app Điện thoại")
        }
    }

    fun openUrl(ctx: Context, url: String) {
        start(ctx, Intent(Intent.ACTION_VIEW, Uri.parse(url)), "Không mở được trình duyệt")
    }

    /** Mở Cài đặt để đăng nhập thêm Gmail. */
    fun addGoogleAccount(ctx: Context) {
        val i = Intent(Settings.ACTION_ADD_ACCOUNT).putExtra(Settings.EXTRA_ACCOUNT_TYPES, arrayOf("com.google"))
        start(ctx, i, "Không mở được Cài đặt tài khoản")
    }

    fun openAppSettings(ctx: Context) {
        val i = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", ctx.packageName, null))
        start(ctx, i, "Không mở được Cài đặt")
    }

    /** Chia sẻ file sao lưu (chọn Google Drive để lưu lên Drive). */
    fun shareFile(ctx: Context, uri: Uri) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "application/octet-stream"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        start(ctx, Intent.createChooser(send, "Lưu file sao lưu vào…"), "Không chia sẻ được file")
    }
}
