package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = IndigoLight,
    onPrimary = Navy900,
    primaryContainer = IndigoDark,
    onPrimaryContainer = Color.White,
    secondary = CyanGlow,
    onSecondary = Navy900,
    secondaryContainer = Navy800,
    onSecondaryContainer = CyanGlow,
    tertiary = EmeraldDelivery,
    background = SlateBackgroundDark,
    surface = SlateSurfaceDark,
    onBackground = SlateTextDarkPrimary,
    onSurface = SlateTextDarkPrimary,
    surfaceVariant = Navy800,
    onSurfaceVariant = SlateTextDarkSecondary,
    outline = SlateBorderDark
)

private val LightColorScheme = lightColorScheme(
    primary = IndigoPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEEF2FF),
    onPrimaryContainer = IndigoDark,
    secondary = CyanAccent,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFECFEFF),
    onSecondaryContainer = Color(0xFF155E75),
    tertiary = EmeraldDelivery,
    background = SlateBackgroundLight,
    surface = SlateSurfaceLight,
    onBackground = SlateTextPrimary,
    onSurface = SlateTextPrimary,
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = SlateTextSecondary,
    outline = SlateBorderLight
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Use our intentional custom palette for cohesive branding
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
