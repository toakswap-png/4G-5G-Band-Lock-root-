package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    background = BackgroundDark,
    surface = SurfaceDark,
    primary = PrimaryBlue,
    onPrimary = OnPrimaryBlue,
    surfaceVariant = SurfaceVariantDark,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    secondary = ActiveBlue,
    onSecondary = PrimaryBlue
)

private val LightColorScheme = DarkColorScheme // Force dark theme for this design

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force dark theme
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Disable dynamic color to enforce our theme
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
