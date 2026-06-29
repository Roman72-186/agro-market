package ru.agromarket.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = AgroPrimary,
    onPrimary = Color.White,
    primaryContainer = AgroPrimaryContainerLight,
    onPrimaryContainer = AgroOnPrimaryContainerLight,
    secondary = AgroSecondary,
    onSecondary = Color.White,
    secondaryContainer = AgroSecondaryContainerLight,
    onSecondaryContainer = AgroOnSecondaryContainerLight,
    tertiary = AgroAccentClay,
    onTertiary = Color.White,
    tertiaryContainer = AgroTertiaryContainerLight,
    onTertiaryContainer = AgroOnTertiaryContainerLight,
    background = AgroNeutralLight,
    onBackground = AgroNeutralDark,
    surface = Color.White,
    onSurface = AgroNeutralDark,
    surfaceVariant = AgroSurfaceVariantLight,
    onSurfaceVariant = AgroOnSurfaceVariantLight,
    outline = AgroOutlineLight,
    outlineVariant = AgroOutlineVariantLight,
    error = AgroRed,
    onError = Color.White,
)

private val DarkColorScheme = darkColorScheme(
    primary = AgroPrimaryDark,
    onPrimary = AgroOnPrimaryDark,
    primaryContainer = AgroPrimaryContainerDark,
    onPrimaryContainer = AgroOnPrimaryContainerDark,
    secondary = AgroSecondaryDark,
    onSecondary = AgroOnSecondaryDark,
    secondaryContainer = AgroSecondaryContainerDark,
    onSecondaryContainer = AgroOnSecondaryContainerDark,
    tertiary = AgroTertiaryDark,
    onTertiary = AgroOnTertiaryDark,
    tertiaryContainer = AgroTertiaryContainerDark,
    onTertiaryContainer = AgroOnTertiaryContainerDark,
    background = AgroBackgroundDark,
    onBackground = AgroOnBackgroundDark,
    surface = AgroSurfaceDark,
    onSurface = AgroOnSurfaceDark,
    surfaceVariant = AgroSurfaceVariantDark,
    onSurfaceVariant = AgroOnSurfaceVariantDark,
    outline = AgroOutlineDark,
    outlineVariant = AgroOutlineVariantDark,
    inverseSurface = AgroInverseSurfaceDark,
    inverseOnSurface = AgroInverseOnSurfaceDark,
)

@Composable
fun AgroMarketTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AgroTypography,
        shapes = AgroShapes,
        content = content,
    )
}
