package com.quoctuan.danhbaso

import com.quoctuan.danhbaso.data.BackupCrypto
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Kiểm tra phần mã hóa file sao lưu.
 * Chạy: chuột phải thư mục app/src/test → Run 'Tests in ...'.
 * Nếu các bài này không qua thì KHÔNG phát hành bản mới (file sao lưu có thể hỏng).
 */
class BackupCryptoTest {

    private val data = """{"app":"danh-ba-so","v":1,"contacts":[{"n":"Mẹ Kiều","p":["0778638687"],"s":true}]}""".toByteArray()

    @Test
    fun maHoaRoiGiaiMa_raDungDuLieuCu() {
        val enc = BackupCrypto.encrypt(data, "MatKhau@123".toCharArray())
        val dec = BackupCrypto.decrypt(enc, "MatKhau@123".toCharArray())
        assertArrayEquals(data, dec)
    }

    @Test
    fun fileCoDauDBS1_vaKhongChuaChuGoc() {
        val enc = BackupCrypto.encrypt(data, "abc12345".toCharArray())
        assertEquals("DBS1", String(enc.copyOfRange(0, 4)))
        assertTrue(!String(enc, Charsets.ISO_8859_1).contains("0778638687"))
    }

    @Test
    fun moiLanMaHoaRaFileKhacNhau() {
        val a = BackupCrypto.encrypt(data, "abc12345".toCharArray())
        val b = BackupCrypto.encrypt(data, "abc12345".toCharArray())
        assertNotEquals(a.toList(), b.toList())
    }

    @Test
    fun saiMatKhau_baoLoi() {
        val enc = BackupCrypto.encrypt(data, "dung-mat-khau".toCharArray())
        try {
            BackupCrypto.decrypt(enc, "sai-mat-khau".toCharArray())
            fail("Sai mật khẩu mà vẫn mở được")
        } catch (e: BackupCrypto.WrongPasswordException) {
        }
    }

    @Test
    fun fileBiSua1Byte_baoLoi() {
        val enc = BackupCrypto.encrypt(data, "abc12345".toCharArray())
        enc[enc.size - 5] = (enc[enc.size - 5].toInt() xor 1).toByte()
        try {
            BackupCrypto.decrypt(enc, "abc12345".toCharArray())
            fail("File bị sửa mà vẫn mở được")
        } catch (e: BackupCrypto.WrongPasswordException) {
        }
    }

    @Test
    fun khongPhaiFileSaoLuu_baoLoi() {
        try {
            BackupCrypto.decrypt(ByteArray(100) { 7 }, "abc12345".toCharArray())
            fail("File rác mà vẫn mở được")
        } catch (e: BackupCrypto.BadFileException) {
        }
    }
}
