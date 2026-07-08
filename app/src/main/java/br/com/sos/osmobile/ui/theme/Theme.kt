package br.com.sos.osmobile.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors: ColorScheme = lightColorScheme(
    primary = Color(0xFF1E6356),
    onPrimary = Color.White,
    secondary = Color(0xFF4C5D70),
    tertiary = Color(0xFF7A4E2E),
    background = Color(0xFFFAFAF7),
    surface = Color(0xFFFFFFFF),
)

private val DarkColors: ColorScheme = darkColorScheme(
    primary = Color(0xFF82D7C4),
    secondary = Color(0xFFB9C7D8),
    tertiary = Color(0xFFE8BE97),
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
