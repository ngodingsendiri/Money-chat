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
# CATATAN: opsi "-keepresources" adalah milik ProGuard dan TIDAK dikenali R8
# (build release gagal dengan "R8: Unknown option"). Resource Firebase
# (default_web_client_id dkk.) dipertahankan lewat dua mekanisme yang valid:
#   1. androidResources.keepSpecificResources di app/build.gradle.kts
#   2. PinConnectScreen membaca default_web_client_id via referensi statis
#      R.string.default_web_client_id (bukan getIdentifier), sehingga resource
#      shrinker otomatis menyimpannya di APK release.

# Data model Firestore dibaca via Firestore.toObject() yang memakai reflection
# (CustomClassMapper). R8 tidak bisa melihat pemakaian reflektif ini, jadi
# field/kelas model cloud wajib di-keep supaya sync cloud tetap jalan di release.
-keep class com.ngodingsendiri.moneychat.data.remote.CloudMessage { *; }
-keep class com.ngodingsendiri.moneychat.data.remote.CloudTransaction { *; }
-keep class com.ngodingsendiri.moneychat.data.local.ChatMessage { *; }
-keep class com.ngodingsendiri.moneychat.data.local.FinancialTransaction { *; }

# Firebase Auth / Firestore: library sudah mengirim consumer rules sendiri,
# jadi tidak perlu keep semua kelas Firebase (biar release tetap ramping).
