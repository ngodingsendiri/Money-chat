package com.startupmini.nyachat.data.backup

/**
 * Sealed Result untuk operasi backup/restore.
 * Menghindari exception yang dikaburkan & memaksa pemanggil handle error eksplisit.
 */
sealed interface BackupResult<out T> {
    /** Sukses dengan nilai [value]. */
    data class Success<out T>(val value: T) : BackupResult<T>

    /** Gagal karena butuh konsen OAuth user. */
    data class ConsentRequired(val intent: android.content.Intent) : BackupResult<Nothing>

    /** Gagal karena error jaringan/auth/timeout. */
    data class Failure(val message: String, val cause: Throwable? = null) : BackupResult<Nothing>

    /** Gagal karena file tidak ditemukan / id tidak valid. */
    data class NotFound(val message: String = "File tidak ditemukan") : BackupResult<Nothing>

    /** Gagal karena quota Drive penuh / batas ukuran. */
    data class QuotaExceeded(val message: String = "Kuota Drive penuh") : BackupResult<Nothing>

    /** Pesan error untuk semua case gagal (bukan Success). */
    val errorMessage: String?
        get() = when (this) {
            is ConsentRequired -> "Perlu persetujuan pengguna"
            is Failure -> message
            is NotFound -> message
            is QuotaExceeded -> message
            else -> null
        }
}