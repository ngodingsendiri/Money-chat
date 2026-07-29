package com.example.data.local.remote

import com.example.BuildConfig
import com.example.data.local.ChatMessage
import com.example.data.local.FinancialTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

data class AiChatParseResult(
    val containsTransaction: Boolean,
    val type: String? = null, // "PENGELUARAN" or "PEMASUKAN"
    val category: String? = null,
    val amount: Double? = null,
    val description: String? = null,
    val aiReply: String
)

object GeminiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private const val MODEL_NAME = "gemini-3.5-flash"

    suspend fun parseChatMessage(
        messageText: String,
        sender: String,
        recentContext: List<ChatMessage>
    ): AiChatParseResult = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY

        // Try Gemini API call if key is available and non-empty
        if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                val prompt = buildParsePrompt(messageText, sender)
                val jsonResponse = callGeminiApi(prompt, apiKey)
                val parsed = parseJsonResponse(jsonResponse, messageText, sender)
                if (parsed != null) {
                    return@withContext parsed
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Fallback to Offline Heuristic Engine
        return@withContext offlineHeuristicParse(messageText, sender)
    }

    suspend fun generateFinancialAuditReport(
        transactions: List<FinancialTransaction>,
        totalIncome: Double,
        totalExpense: Double
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        val balance = totalIncome - totalExpense

        val transSummary = transactions.take(20).joinToString("\n") {
            "- [${it.type}] ${it.category}: Rp ${it.amount.toLong()} (${it.description}) oleh ${it.loggedBy}"
        }

        val prompt = """
            Kamu adalah konsultan dan analis keuangan profesional untuk Money Chat.
            Berikut adalah rekap ringkas pengeluaran dan pemasukan grup/kelompok/keluarga periode ini:
            
            Total Pemasukan: Rp ${totalIncome.toLong()}
            Total Pengeluaran: Rp ${totalExpense.toLong()}
            Sisa Saldo: Rp ${balance.toLong()}
            
            Daftar Transaksi Terakhir:
            $transSummary
            
            Berikan evaluasi kesehatan keuangan ini dalam Bahasa Indonesia yang profesional, obyektif, dan solutif.
            Format tanggapanmu secara terstruktur:
            
            📌 **Evaluasi & Analisis Arus Kas**
            (Analisis jujur dan tajam mengenai rasio pengeluaran vs pemasukan, serta pos belanja paling menonjol)
            
            💡 **Rekomendasi Strategis**
            1. (Saran efisiensi pos pengeluaran operasional/harian)
            2. (Saran alokasi dana cadangan atau perencanaan anggaran ke depan)
            
            Gunakan nada bicara yang profesional, jelas, dan mengedukasi.
        """.trimIndent()

        if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                val jsonResponse = callGeminiApi(prompt, apiKey)
                val text = extractTextFromGeminiResponse(jsonResponse)
                if (!text.isNullOrBlank()) {
                    return@withContext text
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Offline Fallback Report
        return@withContext """
            📌 **Evaluasi & Analisis Arus Kas**
            • **Arus Kas**: Total Pemasukan Rp ${totalIncome.toLong()} vs Pengeluaran Rp ${totalExpense.toLong()} (Sisa Saldo: Rp ${balance.toLong()}).
            • **Tinjauan Obyektif**: ${if (totalExpense > totalIncome && totalIncome > 0) "Pengeluaran melampaui pemasukan! Diperlukan pengetatan pada pos operasional dan belanja non-prioritas." else "Rasio keuangan sehat dan berada dalam batas anggaran aman."}

            💡 **Rekomendasi Strategis**
            1. **Optimalisasi Anggaran Rutin**: Tetapkan batas plafon mingguan untuk pos operasional harian agar alokasi kas terprediksi.
            2. **Alokasi Dana Cadangan**: Sisihkan minimal 10%–15% dari pemasukan ke kas cadangan sebelum memenuhi pengeluaran sekunder.
        """.trimIndent()
    }

    private fun buildParsePrompt(messageText: String, sender: String): String {
        return """
            Kamu adalah 'Asisten Money Chat' yang bertugas memantau obrolan transaksi finansial pada grup, lembaga, atau rumah tangga.
            
            Pesan masuk dari $sender: "$messageText"
            
            Analisis apakah pesan di atas mengandung catatan transaksi, pengeluaran, iuran, tagihan, atau pemasukan dana.
            
            PILIHAN KATEGORI VALID:
            - Groceries & Sembako
            - Makanan & Minuman
            - Tagihan & Utilitas
            - Kebutuhan Operasional
            - Transportasi
            - Kesehatan & Keselamatan
            - Hiburan & Acara
            - Lain-lain
            - Gaji & Pemasukan Kas
            
            Keluarkan jawaban HANYA berupa JSON valid dalam format persis seperti ini:
            {
              "containsTransaction": true,
              "type": "PENGELUARAN" atau "PEMASUKAN",
              "category": "Kebutuhan Operasional",
              "amount": 50000,
              "description": "Beli kertas dan alat tulis",
              "aiReply": "Transaksi 'Beli kertas dan alat tulis' sebesar Rp 50.000 telah dicatat otomatis."
            }
            
            Jika tidak mengandung transaksi keuangan, kirimkan:
            {
              "containsTransaction": false,
              "aiReply": "Catatan pesan tersimpan dalam ruang obrolan."
            }
        """.trimIndent()
    }

    private fun callGeminiApi(prompt: String, apiKey: String): String {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent?key=$apiKey"

        val contentsArray = JSONArray().apply {
            put(JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply {
                        put("text", prompt)
                    })
                })
            })
        }

        val jsonBody = JSONObject().apply {
            put("contents", contentsArray)
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.2)
            })
        }

        val requestBody = jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("HTTP Error: ${response.code} - ${response.message}")
            }
            return response.body?.string() ?: ""
        }
    }

    private fun extractTextFromGeminiResponse(rawJson: String): String? {
        val root = JSONObject(rawJson)
        val candidates = root.optJSONArray("candidates") ?: return null
        if (candidates.length() == 0) return null
        val firstCand = candidates.getJSONObject(0)
        val content = firstCand.optJSONObject("content") ?: return null
        val parts = content.optJSONArray("parts") ?: return null
        if (parts.length() == 0) return null
        return parts.getJSONObject(0).optString("text")
    }

    private fun parseJsonResponse(rawGeminiJson: String, originalText: String, sender: String): AiChatParseResult? {
        val responseText = extractTextFromGeminiResponse(rawGeminiJson) ?: return null
        // Clean JSON formatting
        val cleanedJson = responseText.replace("```json", "").replace("```", "").trim()
        
        return try {
            val json = JSONObject(cleanedJson)
            val contains = json.optBoolean("containsTransaction", false)
            if (contains) {
                AiChatParseResult(
                    containsTransaction = true,
                    type = json.optString("type", "PENGELUARAN"),
                    category = json.optString("category", "Lain-lain"),
                    amount = json.optDouble("amount", 0.0),
                    description = json.optString("description", originalText),
                    aiReply = json.optString("aiReply", "Pesan telah dicatat sebagai transaksi.")
                )
            } else {
                AiChatParseResult(
                    containsTransaction = false,
                    aiReply = json.optString("aiReply", "Pesan tercatat dalam obrolan.")
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun offlineHeuristicParse(messageText: String, sender: String): AiChatParseResult {
        val textLower = messageText.lowercase()

        // Amount detection regex patterns: e.g. "50rb", "50.000", "50000", "50k", "5 juta", "2,5jt"
        var amount: Double? = null
        val numberPattern = Pattern.compile("(\\d+([.,]\\d+)?)\\s*(rb|ribu|k|jt|juta)?")
        val matcher = numberPattern.matcher(textLower)
        
        if (matcher.find()) {
            val numStr = matcher.group(1)?.replace(",", ".") ?: "0"
            val rawNum = numStr.toDoubleOrNull() ?: 0.0
            val unit = matcher.group(3) ?: ""
            
            amount = when (unit) {
                "rb", "ribu", "k" -> rawNum * 1000
                "jt", "juta" -> rawNum * 1000000
                else -> {
                    if (rawNum in 1.0..999.0) rawNum * 1000 else rawNum
                }
            }
        }

        val isIncome = textLower.contains("gaji") || textLower.contains("pemasukan") ||
                textLower.contains("transfer masuk") || textLower.contains("dapat bonus") ||
                textLower.contains("dapat komisi") || textLower.contains("uang jajan masuk")

        val isExpenseTrigger = amount != null && (
                textLower.contains("beli") || textLower.contains("bayar") ||
                textLower.contains("pengeluaran") || textLower.contains("habis") ||
                textLower.contains("belanja") || textLower.contains("ongkir") ||
                textLower.contains("sewa") || textLower.contains("pulsa") ||
                textLower.contains("listrik") || textLower.contains("air") ||
                textLower.contains("popok") || textLower.contains("susu") ||
                textLower.contains("makan") || textLower.contains("transksi") ||
                amount > 0
        )

        if (isIncome && amount != null && amount > 0) {
            return AiChatParseResult(
                containsTransaction = true,
                type = "PEMASUKAN",
                category = "Gaji & Pemasukan",
                amount = amount,
                description = messageText,
                aiReply = "Mantap! Aku catat PEMASUKAN sebesar Rp ${amount.toLong()} (${messageText}). Saldo bertambah! 💰"
            )
        } else if (isExpenseTrigger && amount != null && amount > 0) {
            val category = when {
                textLower.contains("beras") || textLower.contains("minyak") || textLower.contains("sayur") || textLower.contains("sembako") || textLower.contains("pasar") || textLower.contains("supermarket") -> "Groceries & Sembako"
                textLower.contains("makan") || textLower.contains("minum") || textLower.contains("kopi") || textLower.contains("bakso") || textLower.contains("snack") -> "Makanan & Minuman"
                textLower.contains("listrik") || textLower.contains("air") || textLower.contains("wifi") || textLower.contains("pulsa") || textLower.contains("kontrakan") || textLower.contains("pbb") -> "Tagihan & Utilitas"
                textLower.contains("popok") || textLower.contains("susu") || textLower.contains("sekolah") || textLower.contains("mainan") || textLower.contains("anak") -> "Kebutuhan Anak"
                textLower.contains("bensin") || textLower.contains("ojek") || textLower.contains("grab") || textLower.contains("gojek") || textLower.contains("tol") || textLower.contains("parkir") -> "Transportasi"
                textLower.contains("skincare") || textLower.contains("obat") || textLower.contains("dokter") || textLower.contains("sabun") || textLower.contains("shampoo") -> "Kesehatan & Skincare"
                textLower.contains("baju") || textLower.contains("sepatu") || textLower.contains("nonton") || textLower.contains("tas") || textLower.contains("shopee") || textLower.contains("tokped") -> "Hiburan & Belanja"
                else -> "Lain-lain"
            }

            return AiChatParseResult(
                containsTransaction = true,
                type = "PENGELUARAN",
                category = category,
                amount = amount,
                description = messageText,
                aiReply = "Pengeluaran Rp ${amount.toLong()} ($category: $messageText) dicatat oleh $sender."
            )
        }

        return AiChatParseResult(
            containsTransaction = false,
            aiReply = "Tercatat dalam ruang obrolan Money Chat."
        )
    }
}
