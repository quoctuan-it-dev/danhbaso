package com.quoctuan.danhbaso.data

import android.app.KeyguardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Debug
import android.provider.Settings
import java.io.File
import java.security.MessageDigest

/** Mức độ của một vấn đề bảo mật. */
enum class SecLevel { OK, WARN, DANGER }

/** Một mục kiểm tra. [id] dùng để nhớ "Không hỏi lại điểm này nữa". */
data class SecItem(
    val id: String,
    val level: SecLevel,
    val title: String,
    val detail: String,
    /** Màn Cài đặt để khắc phục (null = không có). */
    val settingsAction: String? = null,
)

data class SecReport(val items: List<SecItem>, val signature: String) {
    val danger get() = items.filter { it.level == SecLevel.DANGER }
    val warnings get() = items.filter { it.level == SecLevel.WARN }
}

/**
 * Kiểm tra bảo mật khi mở app. Chạy hoàn toàn trên máy, KHÔNG dùng mạng.
 * Đây không phải phần mềm diệt virus: chỉ phát hiện các dấu hiệu rủi ro phổ biến.
 */
object SecurityCheck {

    /**
     * Mã con dấu (SHA-256 chứng chỉ ký APK) của bản chính chủ.
     * Để TRỐNG = chưa cài đặt, bỏ qua kiểm tra con dấu (lấy mã ở ☰ → Kiểm tra bảo mật rồi dán vào đây).
     * Mã này không phải bí mật: biết mã cũng không làm giả được con dấu.
     */
    private val TRUSTED_SIGNATURES = setOf<String>(
        "82:46:13:F8:9B:04:38:AF:57:0A:1A:F5:78:09:0E:C6:CA:88:5B:FB:F4:56:06:B1:86:95:B3:AA:B9:10:8A:6A",
    )

    /** App trợ năng quen thuộc, an toàn (không cảnh báo). */
    private val SAFE_ACCESSIBILITY = setOf(
        "com.google.android.marvin.talkback",          // TalkBack của Google
        "com.samsung.android.accessibility.talkback",
        "com.google.android.accessibility.soundamplifier",
        "com.google.android.apps.accessibility.voiceaccess",
        "com.google.android.accessibility.switchaccess",
        "com.samsung.accessibility",
    )

    fun run(ctx: Context): SecReport {
        val items = ArrayList<SecItem>()
        val sig = signatureSha256(ctx)

        // 1) App bị sửa / giả mạo
        if (TRUSTED_SIGNATURES.isNotEmpty()) {
            items += if (sig in TRUSTED_SIGNATURES)
                SecItem("sig", SecLevel.OK, "App chính chủ, không bị sửa", "")
            else
                SecItem(
                    "sig", SecLevel.DANGER, "Bản app này đã bị chỉnh sửa",
                    "Không phải bản gốc của Quốc Tuấn. Hãy gỡ app và tải lại bản chính thức tại trang Releases trên GitHub."
                )
        } else {
            items += SecItem("sig", SecLevel.OK, "App chính chủ", "")
        }

        // 2) Root
        items += if (isRooted()) SecItem(
            "root", SecLevel.WARN, "Máy đã bị root",
            "Ứng dụng khác có thể đọc trộm dữ liệu. Không nên tạo file sao lưu trên máy này."
        ) else SecItem("root", SecLevel.OK, "Máy không bị root", "")

        // 3) Khóa màn hình
        val km = ctx.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        items += if (km != null && !km.isDeviceSecure) SecItem(
            "lock", SecLevel.WARN, "Máy chưa đặt khóa màn hình",
            "Ai cầm máy cũng xem được danh bạ. Vào Cài đặt → Bảo mật → Khóa màn hình để đặt mã hoặc vân tay.",
            Settings.ACTION_SECURITY_SETTINGS
        ) else SecItem("lock", SecLevel.OK, "Có khóa màn hình", "")

        // 4) App đang đọc màn hình (Trợ năng) – cách mã độc lừa đảo hay dùng
        val suspicious = accessibilityApps(ctx).filter { it.first !in SAFE_ACCESSIBILITY }
        items += if (suspicious.isNotEmpty()) SecItem(
            "a11y:" + suspicious.joinToString(",") { it.first }.hashCode(),
            SecLevel.WARN, "Có app đang đọc màn hình (Trợ năng)",
            suspicious.joinToString(", ") { "\"${it.second}\"" } +
                " – app này thấy được mọi thứ trên màn hình và tự bấm thay bạn. Nếu không phải app bạn tự cài, hãy tắt hoặc gỡ ngay.",
            Settings.ACTION_ACCESSIBILITY_SETTINGS
        ) else SecItem("a11y", SecLevel.OK, "Không có app lạ đọc màn hình", "")

        // 5) Đang bị gỡ lỗi (debugger) – dấu hiệu có người đang mổ xẻ app
        if (Debug.isDebuggerConnected()) items += SecItem(
            "debugger", SecLevel.WARN, "App đang bị gỡ lỗi (debug)",
            "Có công cụ đang theo dõi app. Nếu bạn không phải người lập trình, hãy rút cáp máy tính."
        )

        // 6) Bản vá bảo mật quá cũ (hơn 2 năm)
        val patch = Build.VERSION.SECURITY_PATCH ?: ""
        val year = patch.take(4).toIntOrNull()
        val nowYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        if (year != null && nowYear - year >= 2) items += SecItem(
            "patch:$patch", SecLevel.WARN, "Bản vá bảo mật Android đã cũ ($patch)",
            "Máy dễ bị tấn công hơn. Vào Cài đặt → Hệ thống → Cập nhật phần mềm nếu có bản mới.",
            Settings.ACTION_DEVICE_INFO_SETTINGS
        )

        return SecReport(items, sig)
    }

    /** SHA-256 của chứng chỉ ký app, dạng AB:CD:... */
    fun signatureSha256(ctx: Context): String = try {
        val pm = ctx.packageManager
        val certs: List<ByteArray> = if (Build.VERSION.SDK_INT >= 28) {
            val info = pm.getPackageInfo(ctx.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            val si = info.signingInfo
            when {
                si == null -> emptyList()
                si.hasMultipleSigners() -> si.apkContentsSigners.map { it.toByteArray() }
                else -> si.signingCertificateHistory.map { it.toByteArray() }
            }
        } else {
            @Suppress("DEPRECATION")
            val info = pm.getPackageInfo(ctx.packageName, PackageManager.GET_SIGNATURES)
            @Suppress("DEPRECATION")
            info.signatures?.map { it.toByteArray() } ?: emptyList()
        }
        val first = certs.lastOrNull() ?: ByteArray(0)
        MessageDigest.getInstance("SHA-256").digest(first).joinToString(":") { "%02X".format(it) }
    } catch (e: Exception) {
        ""
    }

    fun isRooted(): Boolean {
        val tags = Build.TAGS
        if (tags != null && tags.contains("test-keys")) return true
        val paths = listOf(
            "/system/bin/su", "/system/xbin/su", "/sbin/su", "/su/bin/su", "/system/sbin/su",
            "/vendor/bin/su", "/system/app/Superuser.apk", "/data/local/su", "/data/local/bin/su",
            "/data/local/xbin/su", "/data/adb/magisk", "/system/xbin/daemonsu",
        )
        return paths.any { runCatching { File(it).exists() }.getOrDefault(false) }
    }

    /** Các app đang bật Trợ năng: (tên gói, tên hiển thị). */
    private fun accessibilityApps(ctx: Context): List<Pair<String, String>> = try {
        val enabled = Settings.Secure.getInt(ctx.contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED, 0) == 1
        val list = Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        if (!enabled || list.isNullOrBlank()) emptyList()
        else list.split(':').mapNotNull { it.substringBefore('/').takeIf { p -> p.isNotBlank() } }.distinct().map { pkg ->
            val label = try {
                val ai = ctx.packageManager.getApplicationInfo(pkg, 0)
                ctx.packageManager.getApplicationLabel(ai).toString()
            } catch (e: Exception) { pkg }
            pkg to label
        }
    } catch (e: Exception) {
        emptyList()
    }
}
