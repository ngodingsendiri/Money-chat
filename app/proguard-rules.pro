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
