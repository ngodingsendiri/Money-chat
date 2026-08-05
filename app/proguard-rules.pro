# Nyachat — R8 / ProGuard rules
# R8 aktif untuk build release (minify + shrinkResources).

# Simpan line number biar stack trace tetap terbaca (map lewat Play Console).
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# OkHttp / Okio (memakai reflection & service loader)
-dontwarn okhttp3.**
-dontwarn okio.**

# kotlinx.coroutines
-dontwarn kotlinx.coroutines.**

# ---------------------------------------------------------------------------
# Firebase Google Sign-In
# ---------------------------------------------------------------------------
# CATATAN: opsi "-keepresources" adalah milik ProGuard dan TIDAK dikenali R8
# (build release gagal dengan "R8: Unknown option"). Resource Firebase
# (default_web_client_id dkk.) dipertahankan lewat dua mekanisme yang valid:
#   1. app/src/main/res/values/keep.xml (tools:keep) untuk semua resource Firebase
#   2. PinConnectScreen membaca default_web_client_id via getIdentifier() (dengan
#      fallback null) — resource-nya dipertahankan di APK release oleh tools:keep
#      di keep.xml, dan kompil tetap jalan walau oauth_client kosong di JSON.

# Data model Firestore dibaca via Firestore.toObject() yang memakai reflection
# (CustomClassMapper). R8 tidak bisa melihat pemakaian reflektif ini, jadi
# field/kelas model cloud wajib di-keep supaya sync cloud tetap jalan di release.
-keep class com.startupmini.nyachat.data.remote.CloudMessage { *; }
-keep class com.startupmini.nyachat.data.remote.CloudTransaction { *; }
-keep class com.startupmini.nyachat.data.local.ChatMessage { *; }
-keep class com.startupmini.nyachat.data.local.FinancialTransaction { *; }

# Firebase Auth / Firestore: library sudah mengirim consumer rules sendiri,
# jadi tidak perlu keep semua kelas Firebase (biar release tetap ramping).
