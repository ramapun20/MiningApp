package com.example.miningapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF009999),
    secondary = Color(0xFF307D7D),
    tertiary = Color(0xFFCAA85D),
    background = Color.White,
    surface = Color.White,
    onPrimary = Color.White,
    onBackground = Color(0xFF202627),
    onSurface = Color(0xFF202627)
)

@Composable
fun MiningAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}