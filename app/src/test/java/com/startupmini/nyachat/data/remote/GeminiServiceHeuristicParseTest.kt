package com.startupmini.nyachat.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit test mesin parsing heuristik offline (tanpa AI) — dipakai saat tidak ada
 * API key / tidak ada internet. Memverifikasi deteksi nominal, tipe, dan kategori.
 */
class GeminiServiceHeuristicParseTest {

    @Test
    fun deteksiPengeluaranMakanan() {
        val r = GeminiService.offlineHeuristicParse("beli bakso 15000", "Suami")
        assertTrue(r.containsTransaction)
        assertEquals("PENGELUARAN", r.type)
        assertEquals("Makanan & Minuman", r.category)
        assertEquals(15000.0, r.amount!!, 0.001)
    }

    @Test
    fun deteksiPengeluaranListrikRibuan() {
        val r = GeminiService.offlineHeuristicParse("bayar listrik 250rb", "Istri")
        assertTrue(r.containsTransaction)
        assertEquals("PENGELUARAN", r.type)
        assertEquals("Tagihan & Utilitas", r.category)
        assertEquals(250000.0, r.amount!!, 0.001)
    }

    @Test
    fun deteksiGajiJuta() {
        val r = GeminiService.offlineHeuristicParse("gaji sebesar 5 juta", "Suami")
        assertTrue(r.containsTransaction)
        assertEquals("PEMASUKAN", r.type)
        assertEquals(5000000.0, r.amount!!, 0.001)
    }

    @Test
    fun deteksiAngkaDesimalKoma() {
        // "2,5jt" → 2.5 juta
        val r = GeminiService.offlineHeuristicParse("transfer masuk 2,5jt", "Suami")
        assertTrue(r.containsTransaction)
        assertEquals("PEMASUKAN", r.type)
        assertEquals(2500000.0, r.amount!!, 0.001)
    }

    @Test
    fun deteksiRibuanTanpaUnit() {
        // Nominal tanpa unit, 50.000 dianggap ribuan → 50.000
        val r = GeminiService.offlineHeuristicParse("belanja di pasar 50.000", "Suami")
        assertTrue(r.containsTransaction)
        assertEquals(50000.0, r.amount!!, 0.001)
    }

    @Test
    fun pesanBiasaBukanTransaksi() {
        val r = GeminiService.offlineHeuristicParse("halo, apa kabar hari ini?", "Suami")
        assertFalse(r.containsTransaction)
    }

    @Test
    fun deteksiPopokKebutuhanAnak() {
        val r = GeminiService.offlineHeuristicParse("beli popok bayi 45rb", "Istri")
        assertTrue(r.containsTransaction)
        assertEquals("Kebutuhan Anak", r.category)
        assertEquals(45000.0, r.amount!!, 0.001)
    }
}