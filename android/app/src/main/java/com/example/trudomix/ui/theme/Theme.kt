package com.example.trudomix.ui.theme

import android.os.Build
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlueDark,
    secondary = SecondaryTealDark,
    tertiary = TertiaryGreenDark,
    background = BackgroundDark,
    surface = SurfaceDark,
    onBackground = OnSurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = Color(0xFF1D2530),
    onSurfaceVariant = Color(0xFF8795A7),
    outline = Color(0xFF2A313A),
    primaryContainer = Color(0xFF1D2D42),
    onPrimary = Color(0xFF071B32),
    onPrimaryContainer = OnSurfaceDark,
    secondaryContainer = Color(0xFF1D2D42),
    onSecondaryContainer = OnSurfaceDark,
    tertiaryContainer = Color(0xFF173825),
    onTertiaryContainer = OnSurfaceDark,
    error = Color(0xFFF85149),
    errorContainer = Color(0xFF46211F),
    onErrorContainer = Color(0xFFFFDAD6),
    surfaceTint = Color.Transparent,
    surfaceDim = BackgroundDark,
    surfaceBright = Color(0xFF30363D),
    surfaceContainerLowest = BackgroundDark,
    surfaceContainerLow = SurfaceDark,
    surfaceContainer = SurfaceDark,
    surfaceContainerHigh = Color(0xFF21262D),
    surfaceContainerHighest = Color(0xFF30363D),
    outlineVariant = Color(0xFF30363D)
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    secondary = SecondaryTeal,
    tertiary = TertiaryGreen,
    background = BackgroundLight,
    surface = SurfaceLight,
    onBackground = OnSurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = Color(0xFFF1F3F5),
    onSurfaceVariant = Color(0xFF495057),
    outline = Color(0xFF868E96),
    outlineVariant = Color(0xFFE9ECEF),
    primaryContainer = Color(0xFFE7F1FF),
    onPrimary = Color.White,
    onPrimaryContainer = Color(0xFF003D7A),
    secondaryContainer = Color(0xFFE7F1FF),
    onSecondaryContainer = Color(0xFF003D7A),
    tertiaryContainer = Color(0xFFE6F4EA),
    onTertiaryContainer = Color(0xFF205C2D),
    error = Color(0xFFE03131),
    errorContainer = Color(0xFFFFE8E8),
    onErrorContainer = Color(0xFF8C1D1D),
    surfaceTint = Color.Transparent,
    surfaceDim = Color(0xFFE9ECEF),
    surfaceBright = Color.White,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color(0xFFF8F9FA),
    surfaceContainerHighest = Color(0xFFF1F3F5)
)

@Composable
fun TrudomixTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
    ) {
        Surface(Modifier.fillMaxSize(), color = colorScheme.background, contentColor = colorScheme.onBackground, content = content)
    }
}
