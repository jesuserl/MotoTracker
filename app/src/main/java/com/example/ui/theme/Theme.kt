package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = MotoPrimary,
    secondary = MotoSecondaryBg,
    tertiary = MotoAccent,
    background = MotoDarkBg,
    surface = MotoCardBg,
    onPrimary = Color.White,
    onSecondary = MotoTextMain,
    onTertiary = Color(0xFF001D36),
    onBackground = MotoTextMain,
    onSurface = MotoTextMain,
    error = MotoDanger
)

private val DarkColorScheme = darkColorScheme(
    primary = MotoPrimary,
    secondary = MotoSecondaryBg,
    tertiary = MotoAccent,
    background = MotoDarkBg,
    surface = MotoCardBg,
    onPrimary = Color.Black,
    onSecondary = MotoTextMain,
    onTertiary = Color(0xFFD1E4FF),
    onBackground = MotoTextMain,
    onSurface = MotoTextMain,
    error = MotoDanger
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    isDarkModeActiveBySystemOrUser = darkTheme
    
    val colors = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        content = content
    )
}
