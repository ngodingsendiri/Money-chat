package com.startupmini.nyachat.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "chat_messages",
    indices = [Index(value = ["timestamp"]), Index(value = ["cloudId"], unique = true)]
)
data class ChatMessage(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sender: String, // "ISTRI", "SUAMI", "AI"
    val messageText: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isFinancial: Boolean = false,
    val detectedAmount: Double? = null,
    val detectedCategory: String? = null,
    val detectedType: String? = null, // "PENGELUARAN" or "PEMASUKAN"
    val imagePath: String? = null, // path file foto lampiran (nota belanja) di penyimpanan internal
    val filePath: String? = null, // path file dokumen (PDF/invoice/nota) di penyimpanan internal
    val fileName: String? = null, // nama asli file dokumen untuk ditampilkan di bubble
    val replyToSender: String? = null, // snapshot pengirim pesan yang dibalas (balasan via swipe)
    val replyToText: String? = null, // snapshot isi pesan yang dibalas
    val editedAt: Long? = null, // timestamp terakhir diedit (null = belum pernah diedit)
    val cloudId: String? = null // ID dokumen Firestore (unik lintas perangkat)
)
