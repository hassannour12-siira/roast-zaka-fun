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
    primary = FlameOrange,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF3B1506),
    onPrimaryContainer = Color(0xFFFFDBCF),
    secondary = RescueCyan,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF0E3E47),
    onSecondaryContainer = Color(0xFFA6EEFB),
    tertiary = FireAmber,
    background = DeepCharcoal,
    onBackground = OffWhite,
    surface = SurfaceDark,
    onSurface = OffWhite,
    surfaceVariant = Color(0xFF283548),
    onSurfaceVariant = SlateText,
    error = SpicyRed,
    outline = BorderDark
)

private val LightColorScheme = lightColorScheme(
    primary = FlameOrange,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFECE5),
    onPrimaryContainer = Color(0xFF431300),
    secondary = RescueTeal,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCCFBF1),
    onSecondaryContainer = Color(0xFF115E59),
    tertiary = FireAmber,
    background = OffWhite,
    onBackground = DeepCharcoal,
    surface = CardLight,
    onSurface = DeepCharcoal,
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF64748B),
    error = SpicyRed,
    outline = Color(0xFFE2E8F0)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to sleek dark mode for high-contrast fire & rescue aesthetic
    dynamicColor: Boolean = false,
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

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
