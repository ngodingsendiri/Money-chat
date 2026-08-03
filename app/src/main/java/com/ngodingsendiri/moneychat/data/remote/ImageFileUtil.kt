package com.ngodingsendiri.moneychat.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Utilitas foto lampiran (nota belanja):
 * - saveImageFromUri: salin foto dari galeri/kamera, di-downscale & dikompres JPEG
 *   ke penyimpanan internal (filesDir/attachments) supaya ringan & siap dikirim ke AI.
 * - decodeImage: baca bitmap untuk ditampilkan di chat (dengan sampling biar hemat RAM).
 * - encodeBase64: enkode file jadi base64 untuk API AI vision (Gemini inline_data /
 *   OpenRouter image_url).
 */
object ImageFileUtil {

    private const val MAX_DIMENSION = 1600
    private const val JPEG_QUALITY = 85

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
