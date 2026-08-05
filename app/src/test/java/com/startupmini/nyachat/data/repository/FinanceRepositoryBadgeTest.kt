package com.startupmini.nyachat.data.repository

import com.startupmini.nyachat.data.local.ChatMessage
import com.startupmini.nyachat.data.local.FinancialTransaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * P0-2: konsistensi 2 arah — edit transaksi dari Rekap harus memperbarui badge
 * finansial pada pesan chat terkait (nominal/kategori/tipe) supaya kedua layar
 * selalu menampilkan angka yang sama.
 */
class FinanceRepositoryBadgeTest {

    private val message = ChatMessage(
        id = 1L,
        sender = "Suami",
        messageText = "beli bakso 25000",
        isFinancial = true,
        detectedAmount = 25000.0,
        detectedCategory = "Makanan & Minuman",
        detectedType = "PENGELUARAN",
        cloudId = "msg-1"
    )

    private val editedTx = FinancialTransaction(
        type = "PENGELUARAN",
        category = "Transportasi",
        amount = 30000.0,
        description = "beli bakso (dikoreksi: ongkir)",
        loggedBy = "Suami",
        chatMessageId = 1L,
        cloudId = "tx-1"
    )

    @Test
    fun `edit transaksi memperbarui nominal kategori dan tipe di badge pesan`() {
        val badge = editedTx.applyFinancialBadgeTo(message)

        assertTrue(badge.isFinancial)
        assertEquals(30000.0, badge.detectedAmount!!, 0.001)
        assertEquals("Transportasi", badge.detectedCategory)
        assertEquals("PENGELUARAN", badge.detectedType)
        // Field lain tidak berubah
        assertEquals("beli bakso 25000", badge.messageText)
        assertEquals("msg-1", badge.cloudId)
    }

    @Test
    fun `edit transaksi jadi pemasukan memperbarui tipe badge`() {
        val incomeTx = editedTx.copy(type = "PEMASUKAN", category = "Gaji & Pemasukan")
        val badge = incomeTx.applyFinancialBadgeTo(message)

        assertEquals("PEMASUKAN", badge.detectedType)
        assertEquals("Gaji & Pemasukan", badge.detectedCategory)
    }

    @Test
    fun `pesan yang tadinya bukan transaksi jadi bertanda finansial setelah edit`() {
        val plain = ChatMessage(sender = "Suami", messageText = "transfer dulu ya", cloudId = "msg-9")
        val badge = editedTx.applyFinancialBadgeTo(plain)

        assertTrue(badge.isFinancial)
        assertEquals(30000.0, badge.detectedAmount!!, 0.001)
    }

    @Test
    fun `field nullable lain tetap terjaga saat badge diperbarui`() {
        val withReply = message.copy(replyToSender = "Istri", replyToText = "siap", editedAt = 1L)
        val badge = editedTx.applyFinancialBadgeTo(withReply)

        assertEquals("Istri", badge.replyToSender)
        assertEquals(1L, badge.editedAt)
        assertNull(badge.filePath)
    }
}
