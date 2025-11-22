package com.shaarli.poster.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkGreenScheme = darkColorScheme(
    primary = Moss,
    onPrimary = LightText,
    secondary = Pine,
    onSecondary = LightText,
    tertiary = Mint,
    onTertiary = DarkBackground,
    background = DarkBackground,
    onBackground = LightText,
    surface = ForestGreen,
    onSurface = LightText
)

@Composable
fun ShaarliPosterTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkGreenScheme,
        typography = Typography,
        content = content
    )
}
