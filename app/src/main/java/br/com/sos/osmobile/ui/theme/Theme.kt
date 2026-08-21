package br.com.sos.osmobile.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors: ColorScheme = lightColorScheme(
    primary = Color(0xFF006B5B),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFCDEFE7),
    onPrimaryContainer = Color(0xFF073B34),
    secondary = Color(0xFF5A6276),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE1E7F2),
    onSecondaryContainer = Color(0xFF202839),
    tertiary = Color(0xFFB85C38),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDBCB),
    onTertiaryContainer = Color(0xFF422012),
    background = Color(0xFFF4F7FA),
    onBackground = Color(0xFF171C22),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF171C22),
    surfaceVariant = Color(0xFFE7EEF3),
    onSurfaceVariant = Color(0xFF4E5A64),
    outline = Color(0xFF74818B),
    error = Color(0xFFB3261E),
)

private val DarkColors: ColorScheme = darkColorScheme(
    primary = Color(0xFF72D9C5),
    onPrimary = Color(0xFF00382F),
    primaryContainer = Color(0xFF005144),
    onPrimaryContainer = Color(0xFFCDEFE7),
    secondary = Color(0xFFC5CCDA),
    onSecondary = Color(0xFF2C3445),
    secondaryContainer = Color(0xFF424B5E),
    onSecondaryContainer = Color(0xFFE1E7F2),
    tertiary = Color(0xFFFFB59A),
    onTertiary = Color(0xFF652B14),
    tertiaryContainer = Color(0xFF8F4327),
    onTertiaryContainer = Color(0xFFFFDBCB),
    background = Color(0xFF101418),
    onBackground = Color(0xFFE5E8EC),
    surface = Color(0xFF151B21),
    onSurface = Color(0xFFE5E8EC),
    surfaceVariant = Color(0xFF27313A),
    onSurfaceVariant = Color(0xFFC2CAD2),
    outline = Color(0xFF8C98A3),
)

@Composable
fun OSMobileTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
