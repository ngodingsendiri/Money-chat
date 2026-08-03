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

    suspend fun sendMessage(sender: String, messageText: String, imagePath: String? = null) {
        withContext(Dispatchers.IO) {
        // 1. Insert user chat message (cloudId unik lintas perangkat; imagePath = foto nota lokal)
        val initialMsg = ChatMessage(
            sender = sender,
            messageText = messageText,
            timestamp = System.currentTimeMillis(),
            imagePath = imagePath,
            cloudId = UUID.randomUUID().toString()
        )
        val msgId = chatMessageDao.insertMessage(initialMsg)

        // Get recent context
        val recentList = chatMessageDao.getAllMessages().first().takeLast(10)

        // 2. Process message with AI Parser silently in background (foto nota ikut dibaca AI)
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
