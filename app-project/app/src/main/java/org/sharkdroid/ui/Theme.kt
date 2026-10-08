package org.sharkdroid.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

enum class ThemeMode { SYSTEM, LIGHT, DARK }

// "Deep sea" fallback palette (seed #0E7C86) for devices without Material You.
private val LightColors = lightColorScheme(
    primary = Color(0xFF006A6F), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF9CF0F6), onPrimaryContainer = Color(0xFF002022),
    secondary = Color(0xFF4A6365), onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCCE8EA), onSecondaryContainer = Color(0xFF051F21),
    tertiary = Color(0xFF4C5F7C), onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD4E3FF), onTertiaryContainer = Color(0xFF061C36),
    error = Color(0xFFBA1A1A), onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF5FAFB), onBackground = Color(0xFF171D1E),
    surface = Color(0xFFF5FAFB), onSurface = Color(0xFF171D1E),
    surfaceVariant = Color(0xFFDAE4E5), onSurfaceVariant = Color(0xFF3F4849),
    outline = Color(0xFF6F797A), outlineVariant = Color(0xFFBEC8C9),
    inverseSurface = Color(0xFF2B3132), inverseOnSurface = Color(0xFFECF2F2), inversePrimary = Color(0xFF80D4DA),
    surfaceContainerLowest = Color(0xFFFFFFFF), surfaceContainerLow = Color(0xFFEFF5F5),
    surfaceContainer = Color(0xFFE9EFEF), surfaceContainerHigh = Color(0xFFE3E9EA),
    surfaceContainerHighest = Color(0xFFDEE4E4), surfaceDim = Color(0xFFD5DBDC), surfaceBright = Color(0xFFF5FAFB),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF80D4DA), onPrimary = Color(0xFF00373A),
    primaryContainer = Color(0xFF004F53), onPrimaryContainer = Color(0xFF9CF0F6),
    secondary = Color(0xFFB1CBCE), onSecondary = Color(0xFF1C3437),
    secondaryContainer = Color(0xFF324B4D), onSecondaryContainer = Color(0xFFCCE8EA),
    tertiary = Color(0xFFB4C8E9), onTertiary = Color(0xFF1E314C),
    tertiaryContainer = Color(0xFF354863), onTertiaryContainer = Color(0xFFD4E3FF),
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0E1415), onBackground = Color(0xFFDEE4E4),
    surface = Color(0xFF0E1415), onSurface = Color(0xFFDEE4E4),
    surfaceVariant = Color(0xFF3F4849), onSurfaceVariant = Color(0xFFBEC8C9),
    outline = Color(0xFF899393), outlineVariant = Color(0xFF3F4849),
    inverseSurface = Color(0xFFDEE4E4), inverseOnSurface = Color(0xFF2B3132), inversePrimary = Color(0xFF006A6F),
    surfaceContainerLowest = Color(0xFF090F10), surfaceContainerLow = Color(0xFF171D1E),
    surfaceContainer = Color(0xFF1B2122), surfaceContainerHigh = Color(0xFF252B2C),
    surfaceContainerHighest = Color(0xFF303637), surfaceDim = Color(0xFF0E1415), surfaceBright = Color(0xFF343A3B),
)

/** Colours Material 3 has no slot for: filter validation and the "recording" accent. */
@Immutable
data class ExtraColors(
    val validContainer: Color,
    val onValidContainer: Color,
    val recording: Color,
    val dark: Boolean,
)

private val LightExtra = ExtraColors(Color(0xFFC6F0CC), Color(0xFF00210B), Color(0xFFD32F2F), false)
private val DarkExtra = ExtraColors(Color(0xFF1E4A29), Color(0xFFB7F1C1), Color(0xFFFF6E6E), true)

val LocalExtraColors = staticCompositionLocalOf { LightExtra }

object Mono {
    val small = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, lineHeight = 16.sp)
    val body = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp, lineHeight = 18.sp)
    val stat = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium)
}

@Composable
fun SharkTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val ctx = LocalContext.current
    val scheme: ColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        dark -> DarkColors
        else -> LightColors
    }
    CompositionLocalProvider(LocalExtraColors provides if (dark) DarkExtra else LightExtra) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
