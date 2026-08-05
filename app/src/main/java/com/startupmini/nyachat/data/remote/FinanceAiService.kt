package com.startupmini.nyachat.data.remote

import com.startupmini.nyachat.data.local.ChatMessage
import com.startupmini.nyachat.data.local.FinancialTransaction

/**
 * Lapisan AI — hasil dekomposisi `FinanceRepository` (P3-1).
 *
 * Sebelumnya repository memanggil `GeminiService` langsung sehingga mencampur
 * tiga tanggung jawab: persisten lokal, sinkronisasi cloud, dan AI. Dengan
 * layanan ini, repository cukup bergantung pada satu dependency AI yang bisa
 * di-mock di unit test (produksi tetap memakai Gemini/OpenRouter BYOK lewat
 * [GeminiService]).
 */
class FinanceAiService {

    /** Parse pesan chat → transaksi (teks biasa / foto nota). */
    suspend fun parseMessage(
        messageText: String,
        sender: String,
        recentContext: List<ChatMessage>,
        imagePath: String? = null
    ): AiChatParseResult =
        GeminiService.parseChatMessage(messageText, sender, recentContext, imagePath)

    /** Jawaban AI bebas untuk tombol ✨ Tanya AI (bukan parser transaksi). */
    suspend fun askInChat(prompt: String): String =
        GeminiService.askAiChat(prompt)

    /** Saran prompt cepat berdasarkan riwayat transaksi. */
    suspend fun frequentSuggestions(transactions: List<FinancialTransaction>): List<String> =
        GeminiService.generateFrequentTransactionSuggestions(transactions)

    /** Laporan audit keuangan. */
    suspend fun auditReport(
        transactions: List<FinancialTransaction>,
        income: Double,
        expense: Double
    ): String =
        GeminiService.generateFinancialAuditReport(transactions, income, expense)

    /** Analisis bulanan (rekap per bulan + tren + rekomendasi). */
    suspend fun monthlyAnalysis(transactions: List<FinancialTransaction>): String =
        GeminiService.generateMonthlyAnalysisReport(transactions)
}
