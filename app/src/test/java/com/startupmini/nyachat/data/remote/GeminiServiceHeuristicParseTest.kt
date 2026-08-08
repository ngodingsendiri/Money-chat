package com.startupmini.nyachat.data.remote

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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

    // ---- K1: nominal ribuan bertitik bertingkat (≥ 1 juta) ----

    @Test
    fun deteksiNominalJutaBertitikGanda() {
        // Bug lama: "(\\d+(?:[.,]\\d+)?)" hanya mengekstrak grup pertama "1.500",
        // menyisakan ".000" → dianggap Rp 1.500 (salah total). Sekarang harus 1.500.000.
        val r = GeminiService.offlineHeuristicParse("gaji tanggal 1.500.000", "Suami")
        assertTrue(r.containsTransaction)
        assertEquals("PEMASUKAN", r.type)
        assertEquals(1500000.0, r.amount!!, 0.001)
    }

    @Test
    fun extractAmountMenerimaJutaBertitik() {
        assertEquals(1_500_000.0, GeminiService.extractAmountFromText("gaji 1.500.000")!!, 0.001)
        assertEquals(15_000_000.0, GeminiService.extractAmountFromText("beli motor 15.000.000")!!, 0.001)
        assertEquals(1_200.0, GeminiService.extractAmountFromText("bayar 1.200")!!, 0.001)
        assertEquals(2_500_000.0, GeminiService.extractAmountFromText("transfer 2,5jt")!!, 0.001)
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

    // ---- Sprint-1 fix: angka bersatuan dimenangkan atas angka polos pertama ----

    @Test
    fun angkaBersatuanDimenangkanAtasKuantitasDiDepannya() {
        // Bug lama: angka pertama "2" diambil -> Rp 2.000. Sekarang 20rb -> Rp 20.000.
        val r = GeminiService.offlineHeuristicParse("beli 2 kopi 20rb", "Suami")
        assertTrue(r.containsTransaction)
        assertEquals("PENGELUARAN", r.type)
        assertEquals("Makanan & Minuman", r.category)
        assertEquals(20000.0, r.amount!!, 0.001)
    }

    @Test
    fun angkaBersatuanKDimenangkanAtasKuantitas() {
        val r = GeminiService.offlineHeuristicParse("beli 3 botol minum 10k", "Istri")
        assertTrue(r.containsTransaction)
        assertEquals(10000.0, r.amount!!, 0.001)
    }

    @Test
    fun angkaBersatuanPertamaYangMenangBilaAdaBeberapa() {
        val r = GeminiService.offlineHeuristicParse("bayar 2 juta lalu beli kopi 20rb", "Suami")
        assertTrue(r.containsTransaction)
        assertEquals(2000000.0, r.amount!!, 0.001)
    }

    @Test
    fun hurufKataBukanSatuanRibuan() {
        // 'k' pada "kopi" tidak boleh terbaca sebagai satuan ribu. Karena "2"
        // adalah angka polos 1 digit (kuantitas, L2), fallback → null.
        assertEquals(null, GeminiService.extractAmountFromText("beli 2 kopi"))
    }

    // ---- L2: angka polos 1 digit tanpa satuan = kuantitas, bukan nominal ----

    @Test
    fun angkaPolosSatuDigitTanpaSatuanDianggapKuantitas() {
        // "makan 2 kucing" → "2" adalah jumlah item, bukan Rp 2.000.
        assertEquals(null, GeminiService.extractAmountFromText("makan 2 kucing"))
        val r = GeminiService.offlineHeuristicParse("makan 2 kucing", "Suami")
        assertFalse(r.containsTransaction)
    }

    @Test
    fun fallbackAngkaPolosKecilTetapDianggapRibuan() {
        val r = GeminiService.offlineHeuristicParse("beli bakso 15", "Suami")
        assertTrue(r.containsTransaction)
        assertEquals(15000.0, r.amount!!, 0.001)
    }

    @Test
    fun tanpaAngkaTidakAdaNominal() {
        assertNull(GeminiService.extractAmountFromText("halo apa kabar"))
    }

    // ---- Sprint-1/2 fix B6: wrapper timeout menyelubungi kaskade AI ----

    @Test
    fun parseChatMessageTanpaKeyJatuhKeHeuristikLewatWrapperTimeout() = runBlocking {
        // Tanpa API key apa pun, kaskade di dalam withTimeoutOrNull langsung
        // null → fallback heuristik. Memastikan wrapper tidak memutus jalur
        // offline maupun menggantung.
        val prevGemini = GeminiService.userApiKey
        val prevOpenRouter = OpenRouterService.userApiKey
        try {
            GeminiService.userApiKey = null
            OpenRouterService.userApiKey = null

            val r = GeminiService.parseChatMessage("beli bakso 15000", "Suami", emptyList())
            assertTrue(r.containsTransaction)
            assertEquals("PENGELUARAN", r.type)
            assertEquals(15000.0, r.amount!!, 0.001)
        } finally {
            GeminiService.userApiKey = prevGemini
            OpenRouterService.userApiKey = prevOpenRouter
        }
    }
}