package com.startupmini.nyachat.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Test formatter nominal input transaksi (audit UI/UX P1.4) — fungsi murni
 * di AddTransactionDialog.kt, tanpa Robolectric.
 */
class AmountFormatterTest {

    @Test
    fun `amountDigitsOnly membuang karakter non-digit`() {
        assertEquals("50000", amountDigitsOnly("50.000"))
        assertEquals("150000", amountDigitsOnly("Rp 150.000"))
        assertEquals("123", amountDigitsOnly("1a2b3"))
        assertEquals("", amountDigitsOnly(""))
    }

    @Test
    fun `formatAmountDisplay memberi grouping ribuan id-ID`() {
        assertEquals("", formatAmountDisplay(""))
        assertEquals("12", formatAmountDisplay("12"))
        assertEquals("500", formatAmountDisplay("500"))
        assertEquals("1.234", formatAmountDisplay("1234"))
        assertEquals("50.000", formatAmountDisplay("50000"))
        assertEquals("1.500.000", formatAmountDisplay("1500000"))
    }

    @Test
    fun `parseAmount men-strip separator sebelum konversi`() {
        assertEquals(50_000.0, parseAmount("50.000")!!, 0.0001)
        assertEquals(50_000.0, parseAmount("50000")!!, 0.0001)
        assertEquals(1_500_000.0, parseAmount("1.500.000")!!, 0.0001)
    }

    @Test
    fun `parseAmount null untuk input kosong atau tidak valid`() {
        assertNull(parseAmount(""))
        assertNull(parseAmount("abc"))
    }
}
