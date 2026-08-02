package com.example.data.repository

import com.example.data.local.ChatMessage
import com.example.data.local.ChatMessageDao
import com.example.data.local.FinancialTransaction
import com.example.data.local.TransactionDao
import com.example.data.remote.GeminiService
import com.example.data.remote.FirestoreSyncManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FinanceRepository(
    private val chatMessageDao: ChatMessageDao,
    private val transactionDao: TransactionDao
) {

    val allMessages: Flow<List<ChatMessage>> = chatMessageDao.getAllMessages()
    val allTransactions: Flow<List<FinancialTransaction>> = transactionDao.getAllTransactions()

    suspend fun sendMessage(sender: String, messageText: String) {
        withContext(Dispatchers.IO) {
        // 1. Insert user chat message
        val initialMsg = ChatMessage(
            sender = sender,
            messageText = messageText,
            timestamp = System.currentTimeMillis()
        )
        val msgId = chatMessageDao.insertMessage(initialMsg)

        // Get recent context
        val recentList = chatMessageDao.getAllMessages().first().takeLast(10)

        // 2. Process message with AI Parser silently in background
        val aiResult = GeminiService.parseChatMessage(messageText, sender, recentList)

        var finalMsg = initialMsg.copy(id = msgId)

        if (aiResult.containsTransaction && aiResult.amount != null && aiResult.amount > 0) {
            val trans = FinancialTransaction(
                type = aiResult.type ?: "PENGELUARAN",
                category = aiResult.category ?: "Lain-lain",
                amount = aiResult.amount,
                description = aiResult.description ?: messageText,
                loggedBy = sender,
                timestamp = System.currentTimeMillis(),
                chatMessageId = msgId
            )
            val transId = transactionDao.insertTransaction(trans)
            FirestoreSyncManager.syncTransaction(trans.copy(id = transId))

            // Update user message with financial badge tags on the message itself
            finalMsg = initialMsg.copy(
                id = msgId,
                isFinancial = true,
                detectedAmount = aiResult.amount,
                detectedCategory = aiResult.category,
                detectedType = aiResult.type
            )
            chatMessageDao.insertMessage(finalMsg)
        }

        // Sync message to Cloud Firestore
        FirestoreSyncManager.syncChatMessage(finalMsg)

        // NO AUTOMATIC AI CHAT BUBBLE HERE! Chat stays clean between Husband & Wife.
    }
        }

    suspend fun askAiInChat(prompt: String): String {
        return withContext(Dispatchers.IO) {
        // Only created when user explicitly requests AI input/advice
        val recentList = chatMessageDao.getAllMessages().first().takeLast(10)
        val aiResult = GeminiService.parseChatMessage(prompt, "PASUTRI", recentList)
        
        val aiMsg = ChatMessage(
            sender = "AI",
            messageText = aiResult.aiReply,
            timestamp = System.currentTimeMillis()
        )
        val msgId = chatMessageDao.insertMessage(aiMsg)
        FirestoreSyncManager.syncChatMessage(aiMsg.copy(id = msgId))

        aiResult.aiReply
        }
    }

    suspend fun addManualTransaction(transaction: FinancialTransaction) {
        withContext(Dispatchers.IO) {
        val id = transactionDao.insertTransaction(transaction)
        FirestoreSyncManager.syncTransaction(transaction.copy(id = id))
        }
    }

    suspend fun deleteTransaction(transaction: FinancialTransaction) {
        withContext(Dispatchers.IO) {
        transactionDao.deleteTransaction(transaction)
        FirestoreSyncManager.deleteTransactionFromCloud(transaction.id)
        }
    }

    suspend fun clearAllData() {
        withContext(Dispatchers.IO) {
        chatMessageDao.deleteAllMessages()
        transactionDao.deleteAllTransactions()
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
