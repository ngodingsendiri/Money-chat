package com.ngodingsendiri.moneychat.data.repository

import com.ngodingsendiri.moneychat.data.local.ChatMessage
import com.ngodingsendiri.moneychat.data.local.ChatMessageDao
import com.ngodingsendiri.moneychat.data.local.FinancialTransaction
import com.ngodingsendiri.moneychat.data.local.TransactionDao
import com.ngodingsendiri.moneychat.data.remote.FirestoreSyncManager
import com.ngodingsendiri.moneychat.data.remote.GeminiService
import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class FinanceRepository(
    private val chatMessageDao: ChatMessageDao,
    private val transactionDao: TransactionDao
) {

    companion object {
        private const val TAG = "FinanceRepository"
    }

    val allMessages: Flow<List<ChatMessage>> = chatMessageDao.getAllMessages()
    val allTransactions: Flow<List<FinancialTransaction>> = transactionDao.getAllTransactions()

    /** Aktifkan sinkronisasi cloud (wajib sudah login Google + listener realtime). */
    suspend fun startCloudSync(pin: String) {
        if (FirestoreSyncManager.isSignedIn()) {
            FirestoreSyncManager.start(pin, chatMessageDao, transactionDao)
        } else {
            Log.w(TAG, "Cloud sync dilewati: belum login dengan akun Google")
        }
    }

    fun stopCloudSync() {
        FirestoreSyncManager.stop()
    }

    suspend fun sendMessage(
        sender: String,
        messageText: String,
        imagePath: String? = null,
        filePath: String? = null,
        fileName: String? = null,
        replyToSender: String? = null,
        replyToText: String? = null
    ) {
        withContext(Dispatchers.IO) {
        // 1. Insert user chat message (cloudId unik lintas perangkat; imagePath = foto
        //    nota lokal; filePath/fileName = dokumen; replyTo* = pesan yang dibalas)
        val initialMsg = ChatMessage(
            sender = sender,
            messageText = messageText,
            timestamp = System.currentTimeMillis(),
            imagePath = imagePath,
            filePath = filePath,
            fileName = fileName,
            replyToSender = replyToSender,
            replyToText = replyToText,
            cloudId = UUID.randomUUID().toString()
        )
        val msgId = chatMessageDao.insertMessage(initialMsg)

        // Get recent context
        val recentList = chatMessageDao.getAllMessages().first().takeLast(10)

        // 2. Process message with AI Parser silently in background (foto nota ikut
        //    dibaca AI; file PDF hanya dilampirkan — teks caption tetap diparse)
        val aiResult = GeminiService.parseChatMessage(messageText, sender, recentList, imagePath)

        var finalMsg = initialMsg.copy(id = msgId)

        if (aiResult.containsTransaction && aiResult.amount != null && aiResult.amount > 0) {
            val trans = FinancialTransaction(
                type = aiResult.type ?: "PENGELUARAN",
                category = aiResult.category ?: "Lain-lain",
                amount = aiResult.amount,
                description = aiResult.description ?: messageText,
                loggedBy = sender,
                timestamp = System.currentTimeMillis(),
                chatMessageId = msgId,
                cloudId = UUID.randomUUID().toString()
            )
            transactionDao.insertTransaction(trans)

            // Update user message with financial badge tags on the message itself
            finalMsg = initialMsg.copy(
                id = msgId,
                isFinancial = true,
                detectedAmount = aiResult.amount,
                detectedCategory = aiResult.category,
                detectedType = aiResult.type
            )
            chatMessageDao.insertMessage(finalMsg)

            // Sync transaksi ke cloud supaya pasangan/keluarga di perangkat lain ikut melihat
            FirestoreSyncManager.syncTransaction(trans)
        }

        // Push ke cloud supaya pasangan/keluarga di perangkat lain ikut melihat
        FirestoreSyncManager.syncMessage(finalMsg)

        // NO AUTOMATIC AI CHAT BUBBLE HERE! Chat stays clean between Husband & Wife.
    }
        }

    /**
     * Edit isi pesan yang sudah terkirim. Teks baru diparse ulang oleh AI dan:
     * - kalau sekarang jadi transaksi → buat transaksi baru,
     * - kalau tetap transaksi → perbarui transaksi lama (Rekap ikut berubah),
     * - kalau sudah bukan transaksi lagi → transaksi terkait dihapus.
     */
    suspend fun editMessage(messageId: Long, newText: String) {
        withContext(Dispatchers.IO) {
            val existing = chatMessageDao.getById(messageId) ?: return@withContext
            val recentList = chatMessageDao.getAllMessages().first().takeLast(10)

            // Parse ulang dengan AI (foto nota tetap ikut dibaca kalau ada)
            val aiResult = GeminiService.parseChatMessage(
                newText, existing.sender, recentList, existing.imagePath
            )
            val isFinancial = aiResult.containsTransaction && aiResult.amount != null && aiResult.amount > 0

            val updated = existing.copy(
                messageText = newText,
                editedAt = System.currentTimeMillis(),
                isFinancial = isFinancial,
                detectedAmount = if (isFinancial) aiResult.amount else null,
                detectedCategory = if (isFinancial) aiResult.category else null,
                detectedType = if (isFinancial) aiResult.type else null
            )
            chatMessageDao.updateMessage(updated)

            // Transaksi yang terkait dengan pesan ini dicari lewat id lokal pesan
            // (FinancialTransaction.chatMessageId menyimpan id ChatMessage).
            val existingTx = transactionDao.getByChatMessageId(existing.id)
            when {
                isFinancial && existingTx != null -> {
                    // Tetap transaksi → perbarui data Rekap
                    val newTx = existingTx.copy(
                        type = updated.detectedType ?: existingTx.type,
                        category = updated.detectedCategory ?: existingTx.category,
                        amount = updated.detectedAmount ?: existingTx.amount,
                        description = newText,
                        loggedBy = existing.sender
                    )
                    transactionDao.updateTransaction(newTx)
                    FirestoreSyncManager.syncTransaction(newTx)
                }
                isFinancial && existingTx == null -> {
                    // Baru jadi transaksi → buat di Rekap
                    val trans = FinancialTransaction(
                        type = updated.detectedType ?: "PENGELUARAN",
                        category = updated.detectedCategory ?: "Lain-lain",
                        amount = updated.detectedAmount ?: 0.0,
                        description = newText,
                        loggedBy = existing.sender,
                        timestamp = existing.timestamp,
                        chatMessageId = messageId,
                        cloudId = UUID.randomUUID().toString()
                    )
                    transactionDao.insertTransaction(trans)
                    FirestoreSyncManager.syncTransaction(trans)
                }
                !isFinancial && existingTx != null -> {
                    // Bukan transaksi lagi → hapus dari Rekap
                    transactionDao.deleteTransaction(existingTx)
                    existingTx.cloudId?.let { FirestoreSyncManager.deleteTransaction(it) }
                }
                else -> { /* tidak ada perubahan transaksi */ }
            }

            FirestoreSyncManager.syncMessage(updated)
        }
    }

    suspend fun askAiInChat(prompt: String): String {
        return withContext(Dispatchers.IO) {
        // Jawaban AI bebas (bukan parser transaksi) saat user menekan tombol ✨ Tanya AI
        val reply = GeminiService.askAiChat(prompt)

        val aiMsg = ChatMessage(
            sender = "AI",
            messageText = reply,
            timestamp = System.currentTimeMillis(),
            cloudId = UUID.randomUUID().toString()
        )
        chatMessageDao.insertMessage(aiMsg)
        FirestoreSyncManager.syncMessage(aiMsg)

        reply
        }
    }

    suspend fun addManualTransaction(transaction: FinancialTransaction) {
        withContext(Dispatchers.IO) {
        val withCloud = transaction.copy(cloudId = transaction.cloudId ?: UUID.randomUUID().toString())
        transactionDao.insertTransaction(withCloud)
        FirestoreSyncManager.syncTransaction(withCloud)
        }
    }

    /** Perbarui transaksi (edit) lalu sinkronkan ke cloud. */
    suspend fun updateTransaction(transaction: FinancialTransaction) {
        withContext(Dispatchers.IO) {
        transactionDao.updateTransaction(transaction)
        transaction.cloudId?.let { FirestoreSyncManager.syncTransaction(transaction) }
        }
    }

    suspend fun deleteChatMessage(messageId: Long) {
        withContext(Dispatchers.IO) {
        val msg = chatMessageDao.getById(messageId) ?: return@withContext
        chatMessageDao.deleteMessage(messageId)
        msg.cloudId?.let { FirestoreSyncManager.deleteMessage(it) }
        }
    }

    suspend fun deleteTransaction(transaction: FinancialTransaction) {
        withContext(Dispatchers.IO) {
        transactionDao.deleteTransaction(transaction)
        transaction.cloudId?.let { FirestoreSyncManager.deleteTransaction(it) }
        }
    }

    suspend fun clearAllData() {
        withContext(Dispatchers.IO) {
        chatMessageDao.deleteAllMessages()
        transactionDao.deleteAllTransactions()
        FirestoreSyncManager.clearFamilyData()
        }
    }

    /**
     * Restore: ganti seluruh data lokal + cloud dengan isi backup.
     * Id lokal dibuat ulang; relasi transaksi -> pesan chat dijaga lewat
     * pemetaan id lama -> id baru. Semua record lalu di-push ke Firestore
     * supaya perangkat lain di workspace ikut menerima hasil restore.
     */
    suspend fun restoreBackup(messages: List<ChatMessage>, transactions: List<FinancialTransaction>) {
        withContext(Dispatchers.IO) {
            chatMessageDao.deleteAllMessages()
            transactionDao.deleteAllTransactions()

            val idMap = mutableMapOf<Long, Long>()
            messages.forEach { m ->
                val newId = chatMessageDao.insertMessage(m.copy(id = 0))
                idMap[m.id] = newId
            }

            transactions.forEach { t ->
                transactionDao.insertTransaction(
                    t.copy(id = 0, chatMessageId = t.chatMessageId?.let { idMap[it] })
                )
            }

            transactions.forEach { t ->
                t.cloudId?.let {
                    FirestoreSyncManager.syncTransaction(
                        t.copy(chatMessageId = t.chatMessageId?.let { oldId -> idMap[oldId] })
                    )
                }
            }
            messages.forEach { m ->
                m.cloudId?.let {
                    FirestoreSyncManager.syncMessage(m.copy(id = idMap[m.id] ?: m.id))
                }
            }
        }
    }


    suspend fun getFrequentTransactionSuggestions(transactions: List<FinancialTransaction>): List<String> {
        return GeminiService.generateFrequentTransactionSuggestions(transactions)
    }

    suspend fun generateAuditReport(
        transactions: List<FinancialTransaction>,
        income: Double,
        expense: Double
    ): String {
        return GeminiService.generateFinancialAuditReport(transactions, income, expense)
    }
}
