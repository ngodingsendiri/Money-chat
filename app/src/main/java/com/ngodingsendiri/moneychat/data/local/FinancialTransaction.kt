package com.ngodingsendiri.moneychat.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "financial_transactions")
data class FinancialTransaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String, // "PENGELUARAN" or "PEMASUKAN"
    val category: String, // "Groceries & Sembako", "Makanan & Minuman", "Tagihan & Utilitas", "Kebutuhan Anak", "Transportasi", "Kesehatan & Skincare", "Hiburan & Belanja", "Lain-lain", "Gaji & Pemasukan"
    val amount: Double,
    val description: String,
    val loggedBy: String, // "ISTRI", "SUAMI", "AI"
    val timestamp: Long = System.currentTimeMillis(),
    val chatMessageId: Long? = null,
    val cloudId: String? = null // ID dokumen Firestore (unik lintas perangkat)
)
