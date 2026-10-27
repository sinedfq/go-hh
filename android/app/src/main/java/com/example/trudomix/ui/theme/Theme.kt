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
    surfaceVariant = Color(0xFF1B1B30),
    onSurfaceVariant = Color(0xFFA8A5BC),
    outline = Color(0xFF303047),
    primaryContainer = Color(0xFF1B1B30),
    onPrimary = Color(0xFF191431),
    onPrimaryContainer = OnSurfaceDark,
    secondaryContainer = Color(0xFF1B1B30),
    onSecondaryContainer = OnSurfaceDark,
    tertiaryContainer = Color(0xFF1B1B30),
    onTertiaryContainer = OnSurfaceDark,
    error = Color(0xFFFF8797),
    errorContainer = Color(0xFF46211F),
    onErrorContainer = Color(0xFFFFDAD6),
    surfaceTint = Color.Transparent,
    surfaceDim = BackgroundDark,
    surfaceBright = Color(0xFF303047),
    surfaceContainerLowest = BackgroundDark,
    surfaceContainerLow = SurfaceDark,
    surfaceContainer = SurfaceDark,
    surfaceContainerHigh = Color(0xFF1B1B30),
    surfaceContainerHighest = Color(0xFF303047),
    outlineVariant = Color(0xFF303047)
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    secondary = SecondaryTeal,
    tertiary = TertiaryGreen,
    background = BackgroundLight,
    surface = SurfaceLight,
    onBackground = OnSurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = Color(0xFFF0EDF7),
    onSurfaceVariant = Color(0xFF68647A),
    outline = Color(0xFFE5E1EF),
    outlineVariant = Color(0xFFE5E1EF),
    primaryContainer = Color(0xFFF0EDF7),
    onPrimary = Color.White,
    onPrimaryContainer = Color(0xFF191431),
    secondaryContainer = Color(0xFFF0EDF7),
    onSecondaryContainer = Color(0xFF191431),
    tertiaryContainer = Color(0xFFF0EDF7),
    onTertiaryContainer = Color(0xFF205C2D),
    error = Color(0xFFC52C42),
    errorContainer = Color(0xFFFFE8E8),
    onErrorContainer = Color(0xFF8C1D1D),
    surfaceTint = Color.Transparent,
    surfaceDim = Color(0xFFE5E1EF),
    surfaceBright = Color.White,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color(0xFFF8F7FC),
    surfaceContainerHighest = Color(0xFFF0EDF7)
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
        shapes = AppShapes,
    ) {
        Surface(Modifier.fillMaxSize(), color = colorScheme.background, contentColor = colorScheme.onBackground, content = content)
    }
}
