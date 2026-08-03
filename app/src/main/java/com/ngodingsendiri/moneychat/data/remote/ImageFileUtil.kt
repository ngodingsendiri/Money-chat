package com.ngodingsendiri.moneychat.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Hasil penyalinan file dokumen: path lokal + nama asli. */
data class SavedFile(val path: String, val name: String)

/**
 * Utilitas lampiran chat (foto nota & dokumen):
 * - saveImageFromUri: salin foto dari galeri/kamera, di-downscale & dikompres JPEG
 *   ke penyimpanan internal (filesDir/attachments) supaya ringan & siap dikirim ke AI.
 * - saveFileFromUri: salin file dokumen (PDF/invoice/nota) apa adanya ke folder yang
 *   sama — tidak dibaca AI, hanya dilampirkan & bisa dibuka.
 * - decodeImage: baca bitmap untuk ditampilkan di chat (dengan sampling biar hemat RAM).
 * - encodeBase64: enkode file jadi base64 untuk API AI vision (Gemini inline_data /
 *   OpenRouter image_url).
 */
object ImageFileUtil {

    private const val MAX_DIMENSION = 1600
    private const val JPEG_QUALITY = 85

    /** Salin file dari URI (dokumen PDF/invoice) ke penyimpanan internal. */
    suspend fun saveFileFromUri(context: Context, uri: Uri): SavedFile? =
        withContext(Dispatchers.IO) {
            runCatching {
                val originalName = queryDisplayName(context, uri)
                    ?: "file_${System.currentTimeMillis()}"
                val safeName = originalName.substringAfterLast('/').take(80)
                    .replace(Regex("[^A-Za-z0-9._-]"), "_")
                val dir = File(context.filesDir, "attachments").apply { mkdirs() }
                val file = File(dir, "doc_${System.currentTimeMillis()}_$safeName")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    file.outputStream().use { output -> input.copyTo(output) }
                } ?: return@runCatching null
                SavedFile(file.absolutePath, safeName)
            }.getOrNull()
        }

    private fun queryDisplayName(context: Context, uri: Uri): String? =
        runCatching {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) cursor.getString(idx) else null
                } else null
            }
        }.getOrNull()

    /** Salin + downscale foto dari URI (galeri/kamera) ke penyimpanan internal. */
    suspend fun saveImageFromUri(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val bitmap = context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input)
            } ?: return@withContext null

            val scaled = scaleDown(bitmap, MAX_DIMENSION)
            val dir = File(context.filesDir, "attachments").apply { mkdirs() }
            val file = File(dir, "att_${System.currentTimeMillis()}.jpg")
            file.outputStream().use { out ->
                scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            }
            if (scaled !== bitmap) scaled.recycle()
            bitmap.recycle()
            file.absolutePath
        }.getOrNull()
    }

    /** Baca bitmap untuk ditampilkan — disampling supaya hemat memori. */
    fun decodeImage(path: String, maxDim: Int = 1024): Bitmap? {
        return runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, bounds)
            var sample = 1
            while (bounds.outWidth / (sample * 2) >= maxDim ||
                bounds.outHeight / (sample * 2) >= maxDim
            ) {
                sample *= 2
            }
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            BitmapFactory.decodeFile(path, opts)
        }.getOrNull()
    }

    /** Enkode file gambar jadi base64 (untuk API AI vision). */
    fun encodeBase64(imagePath: String): String? {
        return runCatching {
            val bytes = File(imagePath).readBytes()
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        }.getOrNull()
    }

    private fun scaleDown(bitmap: Bitmap, maxDim: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        val max = maxOf(w, h)
        if (max <= maxDim) return bitmap
        val ratio = maxDim.toFloat() / max
        return Bitmap.createScaledBitmap(
            bitmap,
            (w * ratio).toInt().coerceAtLeast(1),
            (h * ratio).toInt().coerceAtLeast(1),
            true
        )
    }
}
