package com.quoctuan.danhbaso

import com.quoctuan.danhbaso.data.BackupContact
import com.quoctuan.danhbaso.data.Vcf
import org.junit.Assert.assertEquals
import org.junit.Test

/** Kiểm tra xuất / nhập file VCF. */
class VcfTest {

    @Test
    fun xuatRoiNhap_raDungTenVaSo() {
        val list = listOf(
            BackupContact("Mẹ Kiều", listOf("0778638687", "0912345678"), false),
            BackupContact("Chú Ba; Hàng xóm", listOf("+84901234567"), false),
        )
        val back = Vcf.read(Vcf.write(list))
        assertEquals(2, back.size)
        assertEquals("Mẹ Kiều", back[0].name)
        assertEquals(listOf("0778638687", "0912345678"), back[0].phones)
        assertEquals("Chú Ba; Hàng xóm", back[1].name)
    }

    @Test
    fun docFileAndroidCu_QuotedPrintable() {
        val v21 = """
            BEGIN:VCARD
            VERSION:2.1
            N;CHARSET=UTF-8;ENCODING=QUOTED-PRINTABLE:;M=E1=BA=B9 Ki=E1=BB=81u;;;
            FN;CHARSET=UTF-8;ENCODING=QUOTED-PRINTABLE:M=E1=BA=B9 Ki=E1=BB=81u
            TEL;CELL:077-863-8687
            END:VCARD
        """.trimIndent()
        val back = Vcf.read(v21.toByteArray())
        assertEquals(1, back.size)
        assertEquals("Mẹ Kiều", back[0].name)
        assertEquals(listOf("0778638687"), back[0].phones)
    }

    @Test
    fun boQuaTheKhongCoSo() {
        val v = "BEGIN:VCARD\nVERSION:3.0\nFN:Không số\nEND:VCARD\n"
        assertEquals(0, Vcf.read(v.toByteArray()).size)
    }
}
