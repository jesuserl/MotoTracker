package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
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
    onTertiary = Color(0xFF21005D),
    onBackground = MotoTextMain,
    onSurface = MotoTextMain,
    error = MotoDanger
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
