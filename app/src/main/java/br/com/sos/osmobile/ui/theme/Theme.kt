package br.com.sos.osmobile.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Indigo900 = Color(0xFF1A237E)
private val Indigo700 = Color(0xFF303F9F)
private val Indigo500 = Color(0xFF3F51B5)
private val Indigo100 = Color(0xFFC5CAE9)
private val Indigo50 = Color(0xFFE8EAF6)

private val Slate900 = Color(0xFF0F172A)
private val Slate800 = Color(0xFF1E293B)
private val Slate700 = Color(0xFF334155)
private val Slate500 = Color(0xFF64748B)
private val Slate300 = Color(0xFFCBD5E1)
private val Slate200 = Color(0xFFE2E8F0)
private val Slate100 = Color(0xFFF1F5F9)
private val Slate50 = Color(0xFFF8FAFC)
private val White = Color(0xFFFFFFFF)

private val Emerald600 = Color(0xFF059669)
private val Emerald500 = Color(0xFF10B981)
private val Emerald100 = Color(0xFFD1FAE5)
private val Emerald50 = Color(0xFFECFDF5)

private val Amber600 = Color(0xFFD97706)
private val Amber100 = Color(0xFFFEF3C7)

private val Rose600 = Color(0xFFDC2626)
private val Rose100 = Color(0xFFFEE2E2)

private val LightColors: ColorScheme = lightColorScheme(
    primary = Indigo700,
    onPrimary = White,
    primaryContainer = Indigo100,
    onPrimaryContainer = Indigo900,
    secondary = Slate700,
    onSecondary = White,
    secondaryContainer = Slate200,
    onSecondaryContainer = Slate900,
    tertiary = Emerald600,
    onTertiary = White,
    tertiaryContainer = Emerald100,
    onTertiaryContainer = Color(0xFF064E3B),
    background = Slate50,
    onBackground = Slate900,
    surface = White,
    onSurface = Slate900,
    surfaceVariant = Slate100,
    onSurfaceVariant = Slate500,
    surfaceTint = Indigo500,
    inverseSurface = Slate800,
    inverseOnSurface = Slate100,
    outline = Slate300,
    outlineVariant = Slate200,
    error = Rose600,
    onError = White,
    errorContainer = Rose100,
    onErrorContainer = Color(0xFF7F1D1D),
    scrim = Color(0x99000000),
)

private val DarkColors: ColorScheme = darkColorScheme(
    primary = Indigo100,
    onPrimary = Indigo900,
    primaryContainer = Indigo700,
    onPrimaryContainer = Indigo50,
    secondary = Slate300,
    onSecondary = Slate900,
    secondaryContainer = Slate700,
    onSecondaryContainer = Slate100,
    tertiary = Emerald500,
    onTertiary = Color(0xFF003828),
    tertiaryContainer = Color(0xFF065F46),
    onTertiaryContainer = Emerald100,
    background = Color(0xFF0B1220),
    onBackground = Slate100,
    surface = Color(0xFF111827),
    onSurface = Slate100,
    surfaceVariant = Color(0xFF1F2937),
    onSurfaceVariant = Slate300,
    surfaceTint = Indigo100,
    inverseSurface = Slate100,
    inverseOnSurface = Slate900,
    outline = Slate500,
    outlineVariant = Color(0xFF374151),
    error = Color(0xFFFCA5A5),
    onError = Color(0xFF7F1D1D),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Rose100,
    scrim = Color(0xCC000000),
)

private val OSMobileTypography: Typography = Typography(
    displayLarge = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    displayMedium = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.25).sp),
    displaySmall = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.SemiBold),
    headlineLarge = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold),
    headlineMedium = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
    headlineSmall = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal),
    bodyMedium = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal),
    bodySmall = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium),
)

private val OSMobileShapes: Shapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

data class StatusColors(
    val success: Color,
    val onSuccess: Color,
    val warning: Color,
    val onWarning: Color,
    val info: Color,
    val onInfo: Color,
)

private val LightStatusColors = StatusColors(
    success = Emerald600,
    onSuccess = White,
    warning = Amber600,
    onWarning = White,
    info = Indigo500,
    onInfo = White,
)

private val DarkStatusColors = StatusColors(
    success = Emerald500,
    onSuccess = Color(0xFF003828),
    warning = Color(0xFFFBBF24),
    onWarning = Color(0xFF451A03),
    info = Indigo100,
    onInfo = Indigo900,
)

val LocalStatusColors = staticCompositionLocalOf { LightStatusColors }

@Composable
fun OSMobileTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val statusColors = if (darkTheme) DarkStatusColors else LightStatusColors
    CompositionLocalProvider(LocalStatusColors provides statusColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = OSMobileTypography,
            shapes = OSMobileShapes,
            content = content,
        )
    }
}