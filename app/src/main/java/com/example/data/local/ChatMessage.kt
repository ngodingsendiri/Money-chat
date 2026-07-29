package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sender: String, // "ISTRI", "SUAMI", "AI"
    val messageText: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isFinancial: Boolean = false,
    val detectedAmount: Double? = null,
    val detectedCategory: String? = null,
    val detectedType: String? = null // "PENGELUARAN" or "PEMASUKAN"
)
