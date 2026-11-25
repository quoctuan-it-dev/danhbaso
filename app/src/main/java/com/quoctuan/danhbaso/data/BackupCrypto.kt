package com.quoctuan.danhbaso.data

import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Mã hóa file sao lưu.
 *
 * Cấu trúc file .dbat:
 *   "DBS1" (4 byte) | phiên bản (1 byte) | số vòng lặp (4 byte) | muối (16 byte) | IV (12 byte) | dữ liệu đã mã hóa + thẻ xác thực
 *
 * - Khóa 256 bit được tạo từ mật khẩu bằng PBKDF2-HMAC-SHA256, 600.000 vòng lặp.
 * - Mã hóa AES-256-GCM: vừa giấu nội dung, vừa phát hiện nếu file bị sửa dù chỉ 1 byte.
 * - Phần đầu file (header) cũng được xác thực, nên không ai đổi được số vòng lặp hay muối.
 * - Mật khẩu không được lưu ở đâu cả.
 */
object BackupCrypto {

    private val MAGIC = byteArrayOf('D'.code.toByte(), 'B'.code.toByte(), 'S'.code.toByte(), '1'.code.toByte())
    private const val VERSION: Byte = 1
    private const val ITERATIONS = 600_000
    private const val SALT_LEN = 16
    private const val IV_LEN = 12
    private const val HEADER_LEN = 4 + 1 + 4 + SALT_LEN + IV_LEN

    class WrongPasswordException : Exception("Sai mật khẩu hoặc file đã bị sửa")
    class BadFileException : Exception("Không phải file sao lưu của Danh bạ số")

    fun encrypt(plain: ByteArray, password: CharArray): ByteArray {
        val rnd = SecureRandom()
        val salt = ByteArray(SALT_LEN).also { rnd.nextBytes(it) }
        val iv = ByteArray(IV_LEN).also { rnd.nextBytes(it) }
        val header = ByteBuffer.allocate(HEADER_LEN)
            .put(MAGIC).put(VERSION).putInt(ITERATIONS).put(salt).put(iv)
            .array()
        val key = deriveKey(password, salt, ITERATIONS)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
        cipher.updateAAD(header)
        return header + cipher.doFinal(plain)
    }

    fun decrypt(data: ByteArray, password: CharArray): ByteArray {
        if (data.size < HEADER_LEN + 16) throw BadFileException()
        val buf = ByteBuffer.wrap(data)
        val magic = ByteArray(4).also { buf.get(it) }
        if (!magic.contentEquals(MAGIC)) throw BadFileException()
        val version = buf.get()
        if (version != VERSION) throw BadFileException()
        val iterations = buf.getInt()
        if (iterations < 100_000 || iterations > 10_000_000) throw BadFileException()
        val salt = ByteArray(SALT_LEN).also { buf.get(it) }
        val iv = ByteArray(IV_LEN).also { buf.get(it) }
        val header = data.copyOfRange(0, HEADER_LEN)
        val body = data.copyOfRange(HEADER_LEN, data.size)
        val key = deriveKey(password, salt, iterations)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
        cipher.updateAAD(header)
        return try {
            cipher.doFinal(body)
        } catch (e: AEADBadTagException) {
            throw WrongPasswordException()
        }
    }

    private fun deriveKey(password: CharArray, salt: ByteArray, iterations: Int): SecretKeySpec {
        val spec = PBEKeySpec(password, salt, iterations, 256)
        try {
            val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
            val key = SecretKeySpec(bytes, "AES")
            bytes.fill(0)
            return key
        } finally {
            spec.clearPassword()
        }
    }

    /** Độ mạnh mật khẩu: 0 = yếu ... 4 = rất mạnh. */
    fun strength(pw: String): Int {
        if (pw.length < 8) return 0
        var s = 1
        if (pw.length >= 12) s++
        val kinds = listOf(
            pw.any { it.isLowerCase() }, pw.any { it.isUpperCase() },
            pw.any { it.isDigit() }, pw.any { !it.isLetterOrDigit() }
        ).count { it }
        if (kinds >= 3) s++
        if (kinds == 4 && pw.length >= 10) s++
        return s.coerceAtMost(4)
    }
}
