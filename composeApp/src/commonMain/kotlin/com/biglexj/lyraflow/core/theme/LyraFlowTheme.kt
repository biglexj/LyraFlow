package com.biglexj.lyraflow.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.biglexj.lyraflow.core.config.ThemeMode

private val LightColors = lightColorScheme(
    primary = Color(0xFF7F52FF),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFECE3FF),
    onPrimaryContainer = Color(0xFF24005A),
    secondary = Color(0xFF00A896),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFC3F8EF),
    onSecondaryContainer = Color(0xFF00201B),
    tertiary = Color(0xFFE55353),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDAD8),
    onTertiaryContainer = Color(0xFF410007),
    background = Color(0xFFF9F7FC),
    onBackground = Color(0xFF1C1B20),
    surface = Color(0xFFFDF8FF),
    onSurface = Color(0xFF1C1B20),
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F),
    outline = Color(0xFF7A757F),
    outlineVariant = Color(0xFFCAC4D0),
    error = Color(0xFFE55353),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD8),
    onErrorContainer = Color(0xFF410007),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB499FF),
    onPrimary = Color(0xFF431198),
    primaryContainer = Color(0xFF5B34B6),
    onPrimaryContainer = Color(0xFFECE3FF),
    secondary = Color(0xFF56E3CE),
    onSecondary = Color(0xFF003731),
    secondaryContainer = Color(0xFF005048),
    onSecondaryContainer = Color(0xFFC3F8EF),
    tertiary = Color(0xFFFF8B8B),
    onTertiary = Color(0xFF680010),
    tertiaryContainer = Color(0xFF93001A),
    onTertiaryContainer = Color(0xFFFFDAD8),
    background = Color(0xFF141218),
    onBackground = Color(0xFFE6E1E9),
    surface = Color(0xFF141218),
    onSurface = Color(0xFFE6E1E9),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F),
    error = Color(0xFFFF8B8B),
    onError = Color(0xFF680010),
    errorContainer = Color(0xFF93001A),
    onErrorContainer = Color(0xFFFFDAD8),
)

private val LyraShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

private val LyraTypography = Typography(
    displaySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 36.sp, lineHeight = 42.sp),
    headlineLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 30.sp, lineHeight = 36.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 30.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
)

@Composable
fun LyraFlowTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val useDark = when (mode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    MaterialTheme(
        colorScheme = if (useDark) DarkColors else LightColors,
        typography = LyraTypography,
        shapes = LyraShapes,
        content = content,
    )
}
