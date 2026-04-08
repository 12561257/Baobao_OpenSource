package com.baobao.fatloss.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val FusionColorScheme = lightColorScheme(
    primary = FusionBlack,
    onPrimary = FusionWhite,
    secondary = FusionAccent,
    onSecondary = FusionWhite,
    tertiary = TextTertiary,
    background = FusionBackground,
    onBackground = TextPrimary,
    surface = FusionCard,
    onSurface = TextPrimary,
    surfaceVariant = FusionBackground,
    onSurfaceVariant = TextSecondary,
    outline = FusionBorder,
    outlineVariant = FusionDivider,
)

@Composable
fun FitAssistantTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FusionColorScheme,
        typography = FusionTypography,
        content = content
    )
}
