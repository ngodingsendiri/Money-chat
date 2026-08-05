package com.startupmini.nyachat.data.backup

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Sprint-2: unit test mesin enkripsi backup (BackupCrypto). Iterasi KDF
 * diturunkan ke 1.000 supaya suite tetap cepat — yang diuji adalah alurnya,
 * bukan kekuatan brute-force-nya.
 */
class BackupCryptoTest {

    private val plaintext = """{"app":"Nyachat","format":1,"messages":[]}"""

    @Test
    fun roundTripEnkripsiDekripsiMengembalikanIsiAsli() {
        val envelope = BackupCrypto.encryptToEnvelope(plaintext, "rahasia123", iterations = 1_000)
        assertEquals(plaintext, BackupCrypto.decryptEnvelope(envelope, "rahasia123"))
    }

    @Test
    fun passphraseSalahDitolak() {
        val envelope = BackupCrypto.encryptToEnvelope(plaintext, "rahasia123", iterations = 1_000)
        // GCM auth gagal → null, bukan exception yang bocor ke caller.
        assertNull(BackupCrypto.decryptEnvelope(envelope, "bukan-ini"))
    }

    @Test
    fun dataYangDiutakAtikDitolak() {
        val envelope = BackupCrypto.encryptToEnvelope(plaintext, "rahasia123", iterations = 1_000)
        // Balik satu karakter di payload base64 → integritas GCM rusak.
        val root = JSONObject(envelope)
        val data = root.getString("data")
        val flipped = (if (data[0] == 'A') 'B' else 'A') + data.substring(1)
        root.put("data", flipped)
        assertNull(BackupCrypto.decryptEnvelope(root.toString(), "rahasia123"))
    }

    @Test
    fun deteksiAmplopTerenkripsi() {
        val envelope = BackupCrypto.encryptToEnvelope(plaintext, "rahasia123", iterations = 1_000)
        assertTrue(BackupCrypto.isEncryptedEnvelope(envelope))
        // Backup plaintext v1 & input bukan JSON → bukan amplop.
        assertFalse(BackupCrypto.isEncryptedEnvelope(plaintext))
        assertFalse(BackupCrypto.isEncryptedEnvelope("bukan json sama sekali"))
        assertFalse(BackupCrypto.isEncryptedEnvelope(""))
    }

    @Test
    fun setiapEnkripsiMemakaiSaltDanIvBerbeda() {
        val a = JSONObject(BackupCrypto.encryptToEnvelope(plaintext, "p", iterations = 1_000))
        val b = JSONObject(BackupCrypto.encryptToEnvelope(plaintext, "p", iterations = 1_000))
        // Salt/IV acak per backup — dua amplop tidak boleh identik.
        assertTrue(a.getString("salt") != b.getString("salt"))
        assertTrue(a.getString("iv") != b.getString("iv"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun passphraseKosongDitolak() {
        BackupCrypto.encryptToEnvelope(plaintext, "", iterations = 1_000)
    }

    @Test
    fun versiAmplopMasaDepanDitolak() {
        val envelope = BackupCrypto.encryptToEnvelope(plaintext, "rahasia123", iterations = 1_000)
        val future = JSONObject(envelope).put("envelope", 99).toString()
        // Konsisten dengan kebijakan backup JSON: format lebih baru ditolak,
        // bukan di-parse salah lalu merusak data.
        assertNull(BackupCrypto.decryptEnvelope(future, "rahasia123"))
    }

    @Test
    fun amplopTanpaTandaNyachatDitolak() {
        val envelope = BackupCrypto.encryptToEnvelope(plaintext, "rahasia123", iterations = 1_000)
        val foreign = JSONObject(envelope).put("app", "AplikasiLain").toString()
        assertNull(BackupCrypto.decryptEnvelope(foreign, "rahasia123"))
    }

    @Test
    fun jsonRusakBukanAmplopDanTidakMelempar() {
        assertFalse(BackupCrypto.isEncryptedEnvelope("{"))
        assertNull(BackupCrypto.decryptEnvelope("{", "rahasia123"))
    }

    @Test
    fun iterasiDisimpanDiAmplopDipakaiSaatDekripsi() {
        val envelope = BackupCrypto.encryptToEnvelope(plaintext, "rahasia123", iterations = 2_000)
        assertEquals(2_000, JSONObject(envelope).getInt("iterations"))
        assertNotNull(BackupCrypto.decryptEnvelope(envelope, "rahasia123"))
    }

    @Test
    fun downgradeAlgoritmaLewatHeaderDitolak() {
        // Hardening Sprint-4: amplop tidak bisa memaksa KDF/cipher lain yang
        // lebih lemah lewat header — hanya kombinasi yang didukung yang lolos.
        val envelope = BackupCrypto.encryptToEnvelope(plaintext, "rahasia123", iterations = 1_000)
        val weakKdf = JSONObject(envelope).put("kdf", "PBKDF2WithHmacSHA1").toString()
        assertNull(BackupCrypto.decryptEnvelope(weakKdf, "rahasia123"))
        val weakCipher = JSONObject(envelope).put("cipher", "AES/CBC/NoPadding").toString()
        assertNull(BackupCrypto.decryptEnvelope(weakCipher, "rahasia123"))
    }

    @Test
    fun iterasiAmplopDiLuarRentangWajarDitolak() {
        // Hardening anti-DoS: iterasi dibaca dari amplop (input pihak luar),
        // jadi nilai raksasa harus ditolak SEBELUM KDF berjalan — bukan malah
        // mengunci CPU perangkat berjam-jam saat restore.
        val envelope = BackupCrypto.encryptToEnvelope(plaintext, "rahasia123", iterations = 1_000)
        val huge = JSONObject(envelope).put("iterations", Int.MAX_VALUE).toString()
        assertNull(BackupCrypto.decryptEnvelope(huge, "rahasia123"))
        val zero = JSONObject(envelope).put("iterations", 0).toString()
        assertNull(BackupCrypto.decryptEnvelope(zero, "rahasia123"))
        val negative = JSONObject(envelope).put("iterations", -5).toString()
        assertNull(BackupCrypto.decryptEnvelope(negative, "rahasia123"))
        // Tepat di atas batas juga ditolak, tanpa perlu menjalankan KDF miliaran
        // iterasi di test (cek rentang terjadi SEBELUM deriveKey).
        val overLimit = JSONObject(envelope)
            .put("iterations", BackupCrypto.MAX_DECRYPT_ITERATIONS + 1).toString()
        assertNull(BackupCrypto.decryptEnvelope(overLimit, "rahasia123"))
    }
}
