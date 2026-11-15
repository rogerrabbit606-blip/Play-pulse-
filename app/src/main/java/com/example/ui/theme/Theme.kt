package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val PlayPulseDarkScheme = darkColorScheme(
    primary = ElectricMint,
    onPrimary = OnElectricMint,
    primaryContainer = ElectricMintContainer,
    onPrimaryContainer = ElectricMint,
    secondary = AmberGold,
    onSecondary = OnAmberGold,
    secondaryContainer = AmberGoldContainer,
    onSecondaryContainer = AmberGold,
    tertiary = SkyCyan,
    onTertiary = OnSkyCyan,
    tertiaryContainer = SkyCyanContainer,
    onTertiaryContainer = SkyCyan,
    background = ObsidianBg,
    onBackground = TextPrimaryDark,
    surface = ObsidianSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = ObsidianSurfaceHigh,
    onSurfaceVariant = TextSecondaryDark,
    surfaceContainerLow = ObsidianSurface,
    surfaceContainer = ObsidianSurfaceHigh,
    surfaceContainerHigh = ObsidianSurfaceHighest,
    outline = ObsidianOutline,
    outlineVariant = ObsidianOutline.copy(alpha = 0.55f),
    error = CoralAlert,
    onError = Color.White,
    errorContainer = CoralAlertContainer,
    onErrorContainer = CoralAlert
)

private val PlayPulseLightScheme = lightColorScheme(
    primary = DeepEmerald,
    onPrimary = Color.White,
    primaryContainer = DeepEmeraldContainer,
    onPrimaryContainer = Color(0xFF002116),
    secondary = WarmOchre,
    onSecondary = Color.White,
    secondaryContainer = WarmOchreContainer,
    onSecondaryContainer = Color(0xFF281800),
    tertiary = DeepAzure,
    onTertiary = Color.White,
    tertiaryContainer = DeepAzureContainer,
    onTertiaryContainer = Color(0xFF001E2E),
    background = IvorySlateBg,
    onBackground = TextPrimaryLight,
    surface = CrispWhiteSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = SoftSlateSurfaceHigh,
    onSurfaceVariant = TextSecondaryLight,
    surfaceContainerLow = CrispWhiteSurface,
    surfaceContainer = SoftSlateSurfaceHigh,
    surfaceContainerHigh = Color(0xFFDCE5F0),
    outline = SoftSlateOutline,
    outlineVariant = SoftSlateOutline.copy(alpha = 0.6f),
    error = CrimsonAlert,
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD9),
    onErrorContainer = Color(0xFF410002)
)

val PlayPulseShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun PlayPulseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) PlayPulseDarkScheme else PlayPulseLightScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = PlayPulseTypography,
        shapes = PlayPulseShapes,
        content = content
    )
}
