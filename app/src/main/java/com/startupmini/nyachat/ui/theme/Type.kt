package com.startupmini.nyachat.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.startupmini.nyachat.R

/**
 * Font brand: Plus Jakarta Sans (audit UI/UX P2.7), dimuat lewat Downloadable
 * Fonts (provider GMS — tanpa berkas TTF di APK). Sertifikat provider ada di
 * res/values/font_certs.xml. Bila provider/gagal muat (device tanpa GMS,
 * Robolectric test), Compose otomatis fallback ke font sistem — tidak crash.
 */
private val googleFontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

private val jakartaSans = GoogleFont("Plus Jakarta Sans")

/** Peran display/headline/title — identitas brand. */
val BrandFontFamily = FontFamily(
    Font(googleFont = jakartaSans, fontProvider = googleFontProvider, weight = FontWeight.Normal),
    Font(googleFont = jakartaSans, fontProvider = googleFontProvider, weight = FontWeight.Medium),
    Font(googleFont = jakartaSans, fontProvider = googleFontProvider, weight = FontWeight.SemiBold),
    Font(googleFont = jakartaSans, fontProvider = googleFontProvider, weight = FontWeight.Bold),
    Font(googleFont = jakartaSans, fontProvider = googleFontProvider, weight = FontWeight.ExtraBold)
)

/**
 * Typography M3: peran display, headline & title memakai font brand; peran
 * body & label tetap FontFamily.Default supaya teks panjang (isi chat, laporan
 * AI) memakai font sistem yang paling optimal untuk membaca.
 */
val Typography = Typography(
    displayLarge = TextStyle(fontFamily = BrandFontFamily, fontWeight = FontWeight.Bold, fontSize = 57.sp, lineHeight = 64.sp, letterSpacing = (-0.25).sp),
    displayMedium = TextStyle(fontFamily = BrandFontFamily, fontWeight = FontWeight.Bold, fontSize = 45.sp, lineHeight = 52.sp),
    displaySmall = TextStyle(fontFamily = BrandFontFamily, fontWeight = FontWeight.Bold, fontSize = 36.sp, lineHeight = 44.sp),
    headlineLarge = TextStyle(fontFamily = BrandFontFamily, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontFamily = BrandFontFamily, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 36.sp),
    headlineSmall = TextStyle(fontFamily = BrandFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 32.sp),
    titleLarge = TextStyle(fontFamily = BrandFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = BrandFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.15.sp),
    titleSmall = TextStyle(fontFamily = BrandFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
)
