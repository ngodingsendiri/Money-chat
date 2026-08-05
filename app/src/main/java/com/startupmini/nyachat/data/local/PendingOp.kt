package com.startupmini.nyachat.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Operasi cloud yang belum berhasil disinkronkan (antrian retry offline).
 *
 * Alur: setiap tulis ke Firestore (pesan/transaksi/hapus/clear) yang gagal
 * atau dilakukan saat belum ada workspace aktif akan disimpan di sini sebagai
 * JSON payload, lalu [com.startupmini.nyachat.data.remote.FirestoreSyncManager]
 * mencoba mengurasnya (drain) dengan exponential backoff selama app hidup.
 * Ops tersimpan di disk sehingga aman walau app ditutup — diproses lagi saat
 * workspace yang sama diaktifkan berikutnya.
 *
 * Penting: ops ini selalu dibersihkan saat pindah workspace (PIN berbeda) atau
 * logout+hapus data, supaya tidak pernah ter-replay ke workspace yang salah.
 */
@Entity(tableName = "pending_ops")
data class PendingOp(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    /** Nama jenis operasi, lihat konstanta OP_* di FirestoreSyncManager. */
    val opType: String,
    /** Isi operasi sebagai JSON (CloudMessage / CloudTransaction / cloudId). */
    val payload: String,
    val createdAt: Long = System.currentTimeMillis()
)
