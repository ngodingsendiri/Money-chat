package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF006C53),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF85F8D2),
    onPrimaryContainer = Color(0xFF002117),
    secondary = Color(0xFF4B635B),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCDE8DE),
    onSecondaryContainer = Color(0xFF072019),
    tertiary = Color(0xFFAD7000),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE0B6),
    onTertiaryContainer = Color(0xFF382200),
    background = Color(0xFFFBFDF9),
    onBackground = Color(0xFF191C1B),
    surface = Color(0xFFFBFDF9),
    onSurface = Color(0xFF191C1B),
    surfaceVariant = Color(0xFFDBE5E0),
    onSurfaceVariant = Color(0xFF3F4945),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = Color(0xFF6F7975),
    outlineVariant = Color(0xFFBEC9C4),
    scrim = Color(0xFF000000)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF69EFC4),
    onPrimary = Color(0xFF00382B),
    primaryContainer = Color(0xFF00513E),
    onPrimaryContainer = Color(0xFF85F8D2),
    secondary = Color(0xFFB1CCC3),
    onSecondary = Color(0xFF1D352E),
    secondaryContainer = Color(0xFF334B44),
    onSecondaryContainer = Color(0xFFCDE8DE),
    tertiary = Color(0xFFE4BE7C),
    onTertiary = Color(0xFF3F2D00),
    tertiaryContainer = Color(0xFF5B4300),
    onTertiaryContainer = Color(0xFFFFE0B6),
    background = Color(0xFF101414),
    onBackground = Color(0xFFE1E3E1),
    surface = Color(0xFF101414),
    onSurface = Color(0xFFE1E3E1),
    surfaceVariant = Color(0xFF3F4945),
    onSurfaceVariant = Color(0xFFBEC9C4),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
    outline = Color(0xFF88938E),
    outlineVariant = Color(0xFF3F4945),
    scrim = Color(0xFF000000)
)

@Composable
fun CoupleFinanceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Disabled to ensure we see our custom colors
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
