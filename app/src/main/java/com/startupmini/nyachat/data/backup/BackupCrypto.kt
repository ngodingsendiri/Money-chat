package com.startupmini.nyachat.data.backup

import org.json.JSONObject
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Enkripsi backup sisi klien (Sprint-2): seluruh riwayat chat + keuangan
 * dibungkus amplop terenkripsi SEBELUM diunggah ke Google Drive.
 *
 * Desain:
 * - AES-256-GCM (kerahasiaan + integritas; salah passphrase → gagal otentikasi).
 * - Key diturunkan dari passphrase user dengan PBKDF2-HMAC-SHA256 (OWASP 2023:
 *   minimal 600.000 iterasi). Passphrase TIDAK pernah disimpan di mana pun —
 *   restore wajib memasukkan passphrase yang sama.
 * - Salt & IV acak per backup; AAD mengikat identitas amplop supaya header
 *   tidak bisa ditukar/diutak-atik tanpa terdeteksi.
 * - Murni JVM (javax.crypto + Base64 internal RFC 4648) sehingga bisa
 *   di-unit-test. java.util.Base64 TIDAK dipakai karena baru ada di API 26
 *   (minSdk 24), dan android.util.Base64 tidak tersedia di unit test JVM.
 *
 * Backup lama (plaintext, tanpa amplop) tetap bisa di-restore — deteksi lewat
 * [isEncryptedEnvelope].
 */
object BackupCrypto {

    internal const val ENVELOPE_VERSION = 1
    internal const val KDF = "PBKDF2WithHmacSHA256"
    internal const val CIPHER_NAME = "AES/GCM/NoPadding"
    /** Minimum OWASP 2023 untuk PBKDF2-HMAC-SHA256. */
    internal const val DEFAULT_ITERATIONS = 600_000
    /**
     * Batas atas iterasi KDF yang diterima saat dekripsi. Nilai iterasi dibaca
     * dari amplop (input yang bisa dikontrol pihak lain) — tanpa batas, amplop
     * jahat dengan iterasi miliaran bisa mengunci CPU perangkat berjam-jam
     * (DoS) saat restore. Nilai produksi 600.000; batas ini jauh di atasnya
     * tapi tetap terhingga.
     */
    internal const val MAX_DECRYPT_ITERATIONS = 10_000_000
    private const val GCM_TAG_BITS = 128
    private const val SALT_BYTES = 16
    private const val IV_BYTES = 12
    private const val KEY_BITS = 256
    private const val APP_TAG = "Nyachat"
    /** AAD mengikat identitas amplop — header yang ditukar membuat dekripsi gagal. */
    private val AAD = "Nyachat-backup-envelope:$ENVELOPE_VERSION".toByteArray(Charsets.UTF_8)

    /** True kalau [json] adalah amplop backup terenkripsi Nyachat. */
    fun isEncryptedEnvelope(json: String): Boolean = runCatching {
        val root = JSONObject(json)
        root.optBoolean("encrypted", false) && root.optString("app") == APP_TAG
    }.getOrDefault(false)

    /**
     * Bungkus [plaintext] (JSON backup) menjadi amplop terenkripsi.
     * [iterations] boleh diturunkan hanya untuk keperluan test.
     */
    fun encryptToEnvelope(plaintext: String, passphrase: String, iterations: Int = DEFAULT_ITERATIONS): String {
        require(passphrase.isNotEmpty()) { "Passphrase tidak boleh kosong" }
        require(iterations >= 1) { "Iterasi KDF tidak valid" }
        val random = SecureRandom()
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        val iv = ByteArray(IV_BYTES).also(random::nextBytes)
        val key = deriveKey(passphrase, salt, iterations)

        val cipher = Cipher.getInstance(CIPHER_NAME)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        cipher.updateAAD(AAD)
        val data = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

        return JSONObject()
            .put("app", APP_TAG)
            .put("encrypted", true)
            .put("envelope", ENVELOPE_VERSION)
            .put("kdf", KDF)
            .put("iterations", iterations)
            .put("salt", encodeBase64(salt))
            .put("cipher", CIPHER_NAME)
            .put("iv", encodeBase64(iv))
            .put("data", encodeBase64(data))
            .toString()
    }

    /**
     * Buka amplop terenkripsi. Return null kalau passphrase salah, amplop
     * rusak/diutak-atik, atau versi amplop lebih baru dari yang dipahami
     * (tolak format masa depan — konsisten dengan kebijakan backup JSON).
     */
    fun decryptEnvelope(envelopeJson: String, passphrase: String): String? = runCatching {
        val root = JSONObject(envelopeJson)
        if (root.optString("app") != APP_TAG) return null
        if (!root.optBoolean("encrypted", false)) return null
        val version = root.optInt("envelope", -1)
        if (version < 1 || version > ENVELOPE_VERSION) return null
        // Hardening (Sprint-4): tolak amplop yang mengaku pakai algoritma lain —
        // penyerang tidak bisa menurunkan ke KDF/cipher lemah lewat header amplop.
        if (root.optString("kdf") != KDF) return null
        if (root.optString("cipher") != CIPHER_NAME) return null

        val iterations = root.getInt("iterations")
        // Hardening: tolak iterasi di luar rentang wajar — amplop jahat tidak
        // boleh memaksa KDF berjam-jam (DoS), dan iterasi < 1 tidak valid.
        if (iterations < 1 || iterations > MAX_DECRYPT_ITERATIONS) return null
        val salt = decodeBase64(root.getString("salt"))
        val iv = decodeBase64(root.getString("iv"))
        val data = decodeBase64(root.getString("data"))

        val key = deriveKey(passphrase, salt, iterations)
        val cipher = Cipher.getInstance(CIPHER_NAME)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        cipher.updateAAD(AAD)
        // Passphrase salah → AEADBadTagException → runCatching → null.
        String(cipher.doFinal(data), Charsets.UTF_8)
    }.getOrNull()

    private fun deriveKey(passphrase: String, salt: ByteArray, iterations: Int): SecretKeySpec {
        val spec = PBEKeySpec(passphrase.toCharArray(), salt, iterations, KEY_BITS)
        return try {
            val bytes = SecretKeyFactory.getInstance(KDF).generateSecret(spec).encoded
            SecretKeySpec(bytes, "AES")
        } finally {
            spec.clearPassword()
        }
    }

    // ===== Base64 murni Kotlin (RFC 4648) =====
    // java.util.Base64 butuh API 26 (minSdk 24) dan android.util.Base64 tidak
    // tersedia di unit test JVM — implementasi kecil ini aman di keduanya.

    private const val B64_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
    private val B64_LOOKUP = IntArray(128) { -1 }.also { table ->
        B64_ALPHABET.forEachIndexed { i, c -> table[c.code] = i }
    }

    internal fun encodeBase64(data: ByteArray): String {
        val sb = StringBuilder((data.size + 2) / 3 * 4)
        var i = 0
        while (i < data.size) {
            val b0 = data[i].toInt() and 0xFF
            val b1 = if (i + 1 < data.size) data[i + 1].toInt() and 0xFF else -1
            val b2 = if (i + 2 < data.size) data[i + 2].toInt() and 0xFF else -1
            sb.append(B64_ALPHABET[b0 shr 2])
            sb.append(B64_ALPHABET[((b0 and 0x03) shl 4) or (if (b1 >= 0) b1 shr 4 else 0)])
            sb.append(if (b1 >= 0) B64_ALPHABET[((b1 and 0x0F) shl 2) or (if (b2 >= 0) b2 shr 6 else 0)] else '=')
            sb.append(if (b2 >= 0) B64_ALPHABET[b2 and 0x3F] else '=')
            i += 3
        }
        return sb.toString()
    }

    internal fun decodeBase64(text: String): ByteArray {
        val clean = text.trim().trimEnd('=')
        val out = ByteArray(clean.length * 3 / 4)
        var buffer = 0
        var bits = 0
        var pos = 0
        for (c in clean) {
            val v = if (c.code < 128) B64_LOOKUP[c.code] else -1
            require(v >= 0) { "Karakter Base64 tidak valid" }
            buffer = (buffer shl 6) or v
            bits += 6
            if (bits >= 8) {
                bits -= 8
                out[pos++] = ((buffer shr bits) and 0xFF).toByte()
            }
        }
        return if (pos == out.size) out else out.copyOf(pos)
    }
}
