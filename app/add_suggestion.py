import re

file_path = "app/src/main/java/com/example/data/remote/GeminiService.kt"

with open(file_path, "r") as f:
    content = f.read()

new_func = """
    suspend fun generateFrequentTransactionSuggestions(
        transactions: List<FinancialTransaction>
    ): List<String> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (transactions.isEmpty()) {
            return@withContext listOf("Makan siang 25.000", "Bensin 20.000", "Beli token listrik 50.000")
        }

        val transSummary = transactions.take(30).joinToString("\\n") {
            "- [${it.type}] ${it.description} (Rp ${it.amount.toLong()})"
        }

        val prompt = \"\"\"
            Kamu adalah analis asisten untuk aplikasi pencatat keuangan chat.
            Diberikan daftar transaksi terakhir pengguna di bawah ini:

            $transSummary

            Tugasmu adalah menganalisis kebiasaan transaksi mereka (yang paling berulang/rutin) lalu menghasilkan 4 sampai 5 teks prompt singkat (rekomendasi chat quick-add) yang bisa mereka klik untuk menginput pengeluaran atau pemasukan dengan cepat berdasarkan pola mereka.
            Contoh output yang diharapkan (sesuaikan dengan isi riwayat transaksi pengguna):
            "Beli bensin 25.000"
            "Makan siang 20.000"
            "Bayar listrik 100.000"
            "Belanja sayur 50.000"

            KEMBALIKAN OUTPUTMU SEBAGAI JSON ARRAY STRING SAJA. Contoh: ["Makan 20k", "Bensin 15k"].
            Jangan tambahkan penjelasan apa pun di luar JSON Array.
        \"\"\".trimIndent()

        if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                val jsonResponse = callGeminiApi(prompt, apiKey)
                val text = extractTextFromGeminiResponse(jsonResponse)
                if (!text.isNullOrBlank()) {
                    val cleanedText = text.replace("```json", "").replace("```", "").trim()
                    val jsonArray = JSONArray(cleanedText)
                    val suggestions = mutableListOf<String>()
                    for (i in 0 until jsonArray.length()) {
                        suggestions.add(jsonArray.getString(i))
                    }
                    if (suggestions.isNotEmpty()) return@withContext suggestions
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Fallback offline heuristic
        val fallback = transactions.take(4).map { "${it.description} ${it.amount.toLong()}" }
        return@withContext fallback.ifEmpty { listOf("Makan siang 25000", "Bensin 20000", "Beli token listrik 50000") }
    }

    suspend fun generateFinancialAuditReport("""

content = content.replace("    suspend fun generateFinancialAuditReport(", new_func)

with open(file_path, "w") as f:
    f.write(content)
