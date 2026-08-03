# Money Chat — R8 / ProGuard rules
# R8 aktif untuk build release (minify + shrinkResources).

# Simpan line number biar stack trace tetap terbaca (map lewat Play Console).
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# OkHttp / Okio (memakai reflection & service loader)
-dontwarn okhttp3.**
-dontwarn okio.**

# kotlinx.coroutines
-dontwarn kotlinx.coroutines.**

# androidx.security (EncryptedSharedPreferences/Tink) memakai reflection
-keep class androidx.security.crypto.** { *; }
-dontwarn com.google.crypto.tink.**

# ---------------------------------------------------------------------------
# Firebase Google Sign-In
# ---------------------------------------------------------------------------
# default_web_client_id dibaca lewat Resources.getIdentifier() (dinamis) di
# PinConnectScreen. Resource shrinker tidak bisa mendeteksi pemakaian dinamis
# ini, sehingga OAuth web client ID hilang dari APK release -> login Google
# gagal dengan pesan "Google Sign-In belum dikonfigurasi" walau Google sudah
# diaktifkan di Firebase Console. Keep resource ini untuk semua build release.
-keepresources string/default_web_client_id
-keepresources string/google_app_id
-keepresources string/gcm_defaultSenderId
-keepresources string/google_api_key
-keepresources string/google_storage_bucket
-keepresources string/project_id

# Data model Firestore dibaca via Firestore.toObject() yang memakai reflection
# (CustomClassMapper). R8 tidak bisa melihat pemakaian reflektif ini, jadi
# field/kelas model cloud wajib di-keep supaya sync cloud tetap jalan di release.
-keep class com.ngodingsendiri.moneychat.data.remote.CloudMessage { *; }
-keep class com.ngodingsendiri.moneychat.data.remote.CloudTransaction { *; }
-keep class com.ngodingsendiri.moneychat.data.local.ChatMessage { *; }
-keep class com.ngodingsendiri.moneychat.data.local.FinancialTransaction { *; }

# Firebase Auth / Firestore: library sudah mengirim consumer rules sendiri,
# jadi tidak perlu keep semua kelas Firebase (biar release tetap ramping).
