package com.ngodingsendiri.moneychat.data.backup

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.GoogleAuthException
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** File backup yang tersimpan di folder privat Google Drive (appDataFolder). */
data class DriveBackupFile(
    val fileId: String,
    val name: String,
    val createdTime: String
)

/**
 * Google meminta persetujuan OAuth untuk scope Drive — Intent-nya harus
 * ditampilkan ke user (Activity) lalu aksi diulang setelah disetujui.
 */
class DriveConsentRequired(val intent: Intent) : Exception("Konsen Drive diperlukan")

/**
 * Backup/restore ke Google Drive via Drive REST API v3. File disimpan di
 * appDataFolder — folder privat per aplikasi (tidak terlihat user, tidak
 * memakai kuota Drive-nya). Token OAuth didapat lewat GoogleAuthUtil memakai
 * akun Google yang sudah dipakai login Firebase — tidak butuh backend sendiri.
 */
object DriveBackupManager {

    private const val TAG = "DriveBackup"
    // Penting: GoogleAuthUtil.getToken WAJIB menyertakan scope profile
    // (userinfo.profile) — tanpa itu panggilan token dilempar/dianggap tidak
    // valid dan backup/restore tidak akan pernah jalan.
    private const val DRIVE_SCOPE =
        "oauth2:https://www.googleapis.com/auth/drive.file " +
            "https://www.googleapis.com/auth/drive.appdata " +
            "https://www.googleapis.com/auth/userinfo.profile"
    private const val API_FILES = "https://www.googleapis.com/drive/v3/files"
    private const val UPLOAD_FILES = "https://www.googleapis.com/upload/drive/v3/files"

    private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Minta access token Google untuk scope Drive (harus dipanggil di
     * background thread). Kalau user belum pernah menyetujui akses Drive,
     * Play Services melempar UserRecoverableAuthException berisi Intent konsen
     * — dibungkus jadi DriveConsentRequired supaya UI bisa menampilkannya lalu
     * mencoba lagi.
     */
    suspend fun getAccessToken(context: Context, email: String): String =
        withContext(Dispatchers.IO) {
            try {
                GoogleAuthUtil.getToken(context, email, DRIVE_SCOPE)
            } catch (e: UserRecoverableAuthException) {
                // e.intent bisa null di versi baru play-services — kalau null,
                // cukup laporkan gagal (tidak ada layar konsen yang bisa ditampilkan).
                val intent = e.intent
                if (intent != null) throw DriveConsentRequired(intent)
                throw IllegalStateException("Token Drive gagal: ${e.message}", e)
            } catch (e: GoogleAuthException) {
                throw IllegalStateException("Token Drive gagal: ${e.message}", e)
            }
        }

    /** Buat file baru di appDataFolder lalu unggah isi JSON-nya. */
    suspend fun uploadBackup(
        context: Context,
        token: String,
        fileName: String,
        json: String
    ): Boolean = withContext(Dispatchers.IO) {
        val fileId = runCatching {
            val meta = JSONObject()
                .put("name", fileName)
                .put("parents", JSONArray().put("appDataFolder"))
            val req = Request.Builder()
                .url(API_FILES)
                .addHeader("Authorization", bearer(token))
                .addHeader("Content-Type", "application/json")
                .post(meta.toString().toRequestBody(JSON_MEDIA))
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    Log.w(TAG, "Buat file gagal: ${resp.code} ${resp.body?.string().orEmpty()}")
                    null
                } else {
                    JSONObject(resp.body?.string().orEmpty()).optString("id").ifEmpty { null }
                }
            }
        }.getOrNull()
        if (fileId == null) return@withContext false

        val uploaded = runCatching {
            val req = Request.Builder()
                .url("$UPLOAD_FILES/$fileId?uploadType=media")
                .addHeader("Authorization", bearer(token))
                .addHeader("Content-Type", "application/json")
                .patch(json.toRequestBody(JSON_MEDIA))
                .build()
            client.newCall(req).execute().use { it.isSuccessful }
        }.getOrDefault(false)
        if (!uploaded) Log.w(TAG, "Unggah isi gagal untuk $fileName")
        uploaded
    }

    /** Daftar file backup (paling baru di depan). */
    suspend fun listBackups(context: Context, token: String): List<DriveBackupFile> =
        withContext(Dispatchers.IO) {
            val body = runCatching {
                val req = Request.Builder()
                    .url("$API_FILES?spaces=appDataFolder&orderBy=createdTime%20desc&fields=files(id,name,createdTime)")
                    .addHeader("Authorization", bearer(token))
                    .build()
                client.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) resp.body?.string().orEmpty() else ""
                }
            }.getOrElse { "" }
            if (body.isEmpty()) return@withContext emptyList()
            runCatching {
                val arr = JSONObject(body).optJSONArray("files") ?: JSONArray()
                (0 until arr.length()).map { i ->
                    val o = arr.getJSONObject(i)
                    DriveBackupFile(
                        fileId = o.optString("id"),
                        name = o.optString("name"),
                        createdTime = o.optString("createdTime")
                    )
                }
            }.getOrElse { emptyList() }
        }

    /** Unduh isi file backup (JSON). */
    suspend fun downloadBackup(context: Context, token: String, fileId: String): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                val req = Request.Builder()
                    .url("$API_FILES/$fileId?alt=media")
                    .addHeader("Authorization", bearer(token))
                    .build()
                client.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) resp.body?.string() else null
                }
            }.getOrNull()
        }

    /** Hapus backup lama — sisakan hanya yang terbaru (keep). */
    suspend fun pruneOldBackups(context: Context, token: String, keep: Int) {
        val files = listBackups(context, token)
        if (files.size <= keep) return
        files.drop(keep).forEach { file ->
            runCatching {
                val req = Request.Builder()
                    .url("$API_FILES/${file.fileId}")
                    .addHeader("Authorization", bearer(token))
                    .delete()
                    .build()
                client.newCall(req).execute().close()
            }
        }
    }

    private fun bearer(token: String) = "Bearer $token"
}
