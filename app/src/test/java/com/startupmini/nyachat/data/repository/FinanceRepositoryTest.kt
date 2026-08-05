package com.startupmini.nyachat.data.repository

import com.startupmini.nyachat.Constants
import com.startupmini.nyachat.data.local.ChatMessage
import com.startupmini.nyachat.data.local.FinancialTransaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * P4-5: konsistensi badge finansial pesan ↔ transaksi.
 *
 * Helper murni yang dipakai `FinanceRepository` saat transaksi diedit/dihapus dari
 * layar Rekap — diuji langsung tanpa Room/Firestore supaya regresi mudah terdeteksi:
 * - `applyFinancialBadgeTo`: edit transaksi → badge pesan ikut diperbarui (Rekap = chat).
 * - `clearFinancialBadge`: hapus transaksi → badge pesan dicabut (tidak ada badge hantu).
 */
class FinanceRepositoryTest {

    private val message = ChatMessage(
        id = 1L,
        sender = "Suami",
        messageText = "beli kopi 20rb",
        timestamp = 1L
    )

    private val transaction = FinancialTransaction(
        type = Constants.TransactionTypes.EXPENSE,
        category = Constants.Categories.FOOD,
        amount = 20000.0,
        description = "beli kopi 20rb",
        loggedBy = "Suami",
        timestamp = 1L
    )

    @Test
    fun applyFinancialBadgeMenyinkronkanNilaiTransaksiKePesan() {
        val updated = transaction.applyFinancialBadgeTo(message)

        assertTrue(updated.isFinancial)
        assertEquals(20000.0, updated.detectedAmount!!, 0.001)
        assertEquals(Constants.Categories.FOOD, updated.detectedCategory)
        assertEquals(Constants.TransactionTypes.EXPENSE, updated.detectedType)
        // Field lain pesan tidak berubah.
        assertEquals("beli kopi 20rb", updated.messageText)
        assertEquals("Suami", updated.sender)
        assertEquals(1L, updated.id)
    }

    @Test
    fun clearFinancialBadgeMencabutStatusKeuanganTapiMenyimpanPesan() {
        val financial = message.copy(
            isFinancial = true,
            detectedAmount = 20000.0,
            detectedCategory = Constants.Categories.FOOD,
            detectedType = Constants.TransactionTypes.EXPENSE
        )
        val cleared = financial.clearFinancialBadge()

        assertFalse(cleared.isFinancial)
        assertNull(cleared.detectedAmount)
        assertNull(cleared.detectedCategory)
        assertNull(cleared.detectedType)
        // Pesan tetap ada — hanya badge yang hilang.
        assertEquals("beli kopi 20rb", cleared.messageText)
        assertEquals(1L, cleared.id)
    }

    @Test
    fun badgePemasukanIkutTersinkronDenganTipeDanKategori() {
        val income = transaction.copy(
            type = Constants.TransactionTypes.INCOME,
            category = Constants.Categories.SALARY,
            amount = 5000000.0
        )
        val updated = income.applyFinancialBadgeTo(message)

        assertTrue(updated.isFinancial)
        assertEquals(Constants.TransactionTypes.INCOME, updated.detectedType)
        assertEquals(Constants.Categories.SALARY, updated.detectedCategory)
        assertEquals(5000000.0, updated.detectedAmount!!, 0.001)
    }
}
