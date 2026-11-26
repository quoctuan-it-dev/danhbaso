package com.quoctuan.danhbaso.data

import android.content.Context

/** Cài đặt nhỏ của app (không chứa mật khẩu hay danh bạ). */
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    /** Tài khoản Gmail đang chọn ở vòng tròn góc trên. */
    var account: String?
        get() = sp.getString("account", null)
        set(v) = sp.edit().putString("account", v).apply()

    /** Thời điểm sao lưu gần nhất (mili giây), 0 = chưa sao lưu. */
    var lastBackup: Long
        get() = sp.getLong("last_backup", 0L)
        set(v) = sp.edit().putLong("last_backup", v).apply()

    /** Cỡ bàn phím: 0 = Nhỏ, 1 = Vừa, 2 = Lớn. */
    var keypadSize: Int
        get() = sp.getInt("keypad_size", 1)
        set(v) = sp.edit().putInt("keypad_size", v).apply()

    /** Bộ màu giao diện: 0 Navy biển, 1 Xanh lá rừng, 2 Xanh ngọc, 3 Than chì. */
    var theme: Int
        get() = sp.getInt("theme", 0)
        set(v) = sp.edit().putInt("theme", v).apply()

    var lastBackupName: String?
        get() = sp.getString("last_backup_name", null)
        set(v) = sp.edit().putString("last_backup_name", v).apply()
}
