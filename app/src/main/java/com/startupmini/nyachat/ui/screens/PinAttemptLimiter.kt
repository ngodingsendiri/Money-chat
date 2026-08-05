package com.startupmini.nyachat.ui.screens

/**
 * Rate limiting percobaan PIN join di sisi klien. PIN adalah password bersama
 * workspace — tanpa pembatas, layar join bisa dipakai brute-force mencoba
 * jutaan kombinasi. Lapisan utama tetap persetujuan owner + rules Firestore;
 * limiter ini memperlambat laju percobaan dan mengurangi beban query.
 *
 * Logika murni (tanpa state) — mudah di-unit-test, dipisah dari Composable
 * seperti [MembershipGateLogic].
 */
object PinAttemptLimiter {
    /** Maksimum percobaan yang dihitung dalam satu jendela waktu. */
    const val MAX_ATTEMPTS = 5

    /** Jendela penghitungan percobaan (percobaan lebih lama tidak dihitung). */
    const val WINDOW_MS = 2 * 60 * 1000L

    /** Durasi lockout setelah melewati [MAX_ATTEMPTS] dalam [WINDOW_MS]. */
    const val LOCKOUT_MS = 60 * 1000L

    /**
     * Kapan lockout berakhir (epoch ms), atau null kalau boleh mencoba sekarang.
     * Lockout dihitung dari titik batas percobaan TERLALUI (percobaan ke-5
     * terbaru dalam jendela), BUKAN dari percobaan terakhir — sehingga masa
     * tunggu tidak terus diperpanjang selama jendela masih berisi percobaan.
     */
    fun lockoutEndsAt(attemptTimesMs: List<Long>, now: Long): Long? {
        val recent = attemptTimesMs.filter { it in (now - WINDOW_MS)..now }
        if (recent.size < MAX_ATTEMPTS) return null
        // Percobaan ke-5 terbaru = momen batas terlalui (max dari 5 terbaru).
        val thresholdReachedAt = recent.sortedDescending().take(MAX_ATTEMPTS).max()
        val endsAt = thresholdReachedAt + LOCKOUT_MS
        // Lockout sudah lewat → anggap bebas lagi.
        return if (endsAt <= now) null else endsAt
    }

    /** Sisa detik lockout (dibulatkan ke atas), atau 0 kalau tidak terkunci. */
    fun remainingLockSeconds(attemptTimesMs: List<Long>, now: Long): Long {
        val endsAt = lockoutEndsAt(attemptTimesMs, now) ?: return 0
        return ((endsAt - now) + 999) / 1000
    }
}
