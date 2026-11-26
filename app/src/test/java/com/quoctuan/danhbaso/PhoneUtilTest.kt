package com.quoctuan.danhbaso

import com.quoctuan.danhbaso.util.PhoneUtil
import com.quoctuan.danhbaso.util.TextUtil
import org.junit.Assert.assertEquals
import org.junit.Test

/** Kiểm tra xử lý số điện thoại và chữ tiếng Việt. */
class PhoneUtilTest {

    @Test
    fun local_doi84Thanh0_vaBoKyTuThua() {
        assertEquals("0912345678", PhoneUtil.local("+84 912.345.678"))
        assertEquals("0912345678", PhoneUtil.local("0912-345-678"))
    }

    @Test
    fun key_coiHaiCachVietLaCungMotSo() {
        assertEquals(PhoneUtil.key("0912345678"), PhoneUtil.key("+84912345678"))
    }

    @Test
    fun boDauTiengViet() {
        assertEquals("me kieu", TextUtil.removeDiacritics("Mẹ Kiều").lowercase())
        assertEquals("duong", TextUtil.removeDiacritics("Đường").lowercase())
    }
}
