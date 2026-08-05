package com.startupmini.nyachat.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Audit-fix: rate limiting percobaan PIN join. Logika murni diuji tanpa
 * Compose/Firestore — perilaku kunci: batas percobaan per jendela waktu,
 * lockout sementara, dan percobaan lama yang kedaluwarsa tidak dihitung.
 */
class PinAttemptLimiterTest {

    private val now = 1_000_000L

    @Test
    fun diBawahBatasTidakTerkunci() {
        val attempts = List(PinAttemptLimiter.MAX_ATTEMPTS - 1) { now - it * 1_000L }
        assertNull(PinAttemptLimiter.lockoutEndsAt(attempts, now))
        assertEquals(0, PinAttemptLimiter.remainingLockSeconds(attempts, now))
    }

    @Test
    fun mencapaiBatasDalamJendelaLangsungTerkunci() {
        val attempts = List(PinAttemptLimiter.MAX_ATTEMPTS) { now - it * 1_000L }
        val endsAt = PinAttemptLimiter.lockoutEndsAt(attempts, now)
        // Lockout dihitung dari titik batas terlalui (percobaan terbaru = now).
        assertEquals(now + PinAttemptLimiter.LOCKOUT_MS, endsAt)
        assertEquals(60, PinAttemptLimiter.remainingLockSeconds(attempts, now))
    }

    @Test
    fun percobaanLamaDiLuarJendelaTidakDihitung() {
        val lama = List(PinAttemptLimiter.MAX_ATTEMPTS) { now - PinAttemptLimiter.WINDOW_MS - 1 }
        // Semua percobaan sudah kedaluwarsa → tidak terkunci walau jumlahnya banyak.
        assertNull(PinAttemptLimiter.lockoutEndsAt(lama, now))
        // Ditambah percobaan baru yang masih di bawah batas → tetap bebas.
        val campuran = lama + (now - 1_000L)
        assertNull(PinAttemptLimiter.lockoutEndsAt(campuran, now))
    }

    @Test
    fun lockoutBerakhirSetelahMasaTunggu() {
        val attempts = List(PinAttemptLimiter.MAX_ATTEMPTS) { now }
        val setelahLockout = now + PinAttemptLimiter.LOCKOUT_MS + 1
        assertNull(PinAttemptLimiter.lockoutEndsAt(attempts, setelahLockout))
    }

    @Test
    fun sisaDetikDibulatkanKeAtas() {
        val attempts = List(PinAttemptLimiter.MAX_ATTEMPTS) { now }
        // 59,5 detik tersisa → dibulatkan menjadi 60.
        assertEquals(60, PinAttemptLimiter.remainingLockSeconds(attempts, now + 500))
    }

    @Test
    fun daftarPercobaanKosongTidakTerkunci() {
        assertNull(PinAttemptLimiter.lockoutEndsAt(emptyList(), now))
        assertEquals(0, PinAttemptLimiter.remainingLockSeconds(emptyList(), now))
    }
}
