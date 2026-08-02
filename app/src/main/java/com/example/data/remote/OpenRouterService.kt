package com.example.data.remote

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Layanan AI OpenRouter (BYOK): pengguna menempel API key OpenRouter miliknya
 * sendiri lewat Pengaturan → "Kunci OpenRouter API". Key disimpan lokal di
 * perangkat dan dipakai langsung — server tidak menyediakan API key.
 *
 * Menggunakan model GRATIS dengan rotasi otomatis: kalau satu model kena rate
 * limit (429) / kuota habis / error, otomatis mencoba model gratis berikutnya.
 * Kalau semua gagal, mengembalikan null agar pemanggil fallback ke mesin offline.
 */
object OpenRouterService {

    @Volatile
    var userApiKey: String? = null

    /** Key bawaan aplikasi dari BuildConfig (diisi via env OPENROUTER_API_KEY saat build/CI). */
    private fun appApiKey(): String = BuildConfig.OPENROUTER_API_KEY

    /** Key aktif: key pengguna (BYOK) lebih diutamakan, fallback ke key bawaan app. */
    fun activeApiKey(): String? =
        userApiKey?.takeIf { it.isNotBlank() }
            ?: appApiKey().takeIf { it.isNotBlank() && it != "YOUR_OPENROUTER_API_KEY" }

    private const val BASE_URL = "https://openrouter.ai/api/v1/chat/completions"

    /** Daftar model gratis (divalidasi langsung dari https://openrouter.ai/api/v1/models).
     *  Entri terakhir "openrouter/free" adalah router virtual yang otomatis
     *  memilih model gratis yang sedang tersedia. */
    private val FREE_MODELS = listOf(
        "inclusionai/ling-3.0-flash:free",
        "openai/gpt-oss-20b:free",
        "google/gemma-4-31b-it:free",
        "nvidia/nemotron-3-ultra-550b-a55b:free",
        "poolside/laguna-xs-2.1:free",
        "openrouter/free",
    )

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /** Kirim prompt ke model gratis OpenRouter dengan rotasi otomatis saat gagal. */
    suspend fun completeChat(prompt: String): String? = withContext(Dispatchers.IO) {
        val key = activeApiKey() ?: return@withContext null

        for (model in FREE_MODELS) {
            try {
                val text = tryModel(key, model, prompt)
                if (!text.isNullOrBlank()) return@withContext text
            } catch (e: Exception) {
                e.printStackTrace() // model ini gagal → lanjut ke model berikutnya
            }
        }
        null
    }

    private fun tryModel(key: String, model: String, prompt: String): String? {
        val body = JSONObject()
            .put("model", model)
            .put("messages", JSONArray().put(
                JSONObject().put("role", "user").put("content", prompt)
            ))
            .put("temperature", 0.2)

        val request = Request.Builder()
            .url(BASE_URL)
            .header("Authorization", "Bearer $key")
            .post(body.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                // 429 (rate limit) / 402 (saldo habis) / 404 (model tak ada) → lempar agar dirotasi
                throw Exception("HTTP ${response.code}: ${response.message}")
            }
            val raw = response.body?.string() ?: return null
            val root = JSONObject(raw)
            val choices = root.optJSONArray("choices") ?: return null
            if (choices.length() == 0) return null
            return choices.getJSONObject(0)
                .optJSONObject("message")
                ?.optString("content", "")
        }
    }
}
