package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalArisePalette = staticCompositionLocalOf { ArisePalettes.ElectricCyan }
val LocalVisualStyle = staticCompositionLocalOf { VisualStyle.SCI_FI_FUTURISTIC }
val LocalLayoutDensity = staticCompositionLocalOf { LayoutDensity.COMFORTABLE }

object AriseThemeTokens {
    val palette: ArisePalette
        @Composable
        @ReadOnlyComposable
        get() = LocalArisePalette.current

    val visualStyle: VisualStyle
        @Composable
        @ReadOnlyComposable
        get() = LocalVisualStyle.current

    val layoutDensity: LayoutDensity
        @Composable
        @ReadOnlyComposable
        get() = LocalLayoutDensity.current
}

@Composable
fun AriseTheme(
    config: ThemeConfiguration = ThemeConfiguration(),
    accentColor: Color? = null,
    content: @Composable () -> Unit
) {
    val resolvedPalette = ThemeManager.resolveActivePalette(config).let { base ->
        if (accentColor != null) base.copy(primary = accentColor, accent = accentColor) else base
    }

    val isDark = when (config.appearanceMode) {
        AppearanceMode.LIGHT -> false
        AppearanceMode.DARK, AppearanceMode.AMOLED_BLACK, AppearanceMode.HIGH_CONTRAST -> true
        AppearanceMode.SYSTEM_DEFAULT -> isSystemInDarkTheme()
        AppearanceMode.CUSTOM -> !resolvedPalette.isLight
    }

    val colorScheme: ColorScheme = if (isDark) {
        darkColorScheme(
            primary = resolvedPalette.primary,
            onPrimary = if (resolvedPalette.isLight) Color.White else Color.Black,
            primaryContainer = resolvedPalette.secondary.copy(alpha = 0.35f),
            onPrimaryContainer = resolvedPalette.textPrimary,
            secondary = resolvedPalette.secondary,
            onSecondary = Color.White,
            secondaryContainer = resolvedPalette.surfaceVariant,
            onSecondaryContainer = resolvedPalette.textPrimary,
            tertiary = resolvedPalette.accent,
            onTertiary = Color.Black,
            background = resolvedPalette.background,
            onBackground = resolvedPalette.textPrimary,
            surface = resolvedPalette.surface,
            onSurface = resolvedPalette.textPrimary,
            surfaceVariant = resolvedPalette.surfaceVariant,
            onSurfaceVariant = resolvedPalette.textSecondary,
            outline = resolvedPalette.cardBorder
        )
    } else {
        lightColorScheme(
            primary = resolvedPalette.primary,
            onPrimary = Color.White,
            primaryContainer = resolvedPalette.primary.copy(alpha = 0.15f),
            onPrimaryContainer = resolvedPalette.primary,
            secondary = resolvedPalette.secondary,
            onSecondary = Color.White,
            secondaryContainer = resolvedPalette.surfaceVariant,
            onSecondaryContainer = resolvedPalette.textPrimary,
            tertiary = resolvedPalette.accent,
            onTertiary = Color.White,
            background = resolvedPalette.background,
            onBackground = resolvedPalette.textPrimary,
            surface = resolvedPalette.surface,
            onSurface = resolvedPalette.textPrimary,
            surfaceVariant = resolvedPalette.surfaceVariant,
            onSurfaceVariant = resolvedPalette.textSecondary,
            outline = resolvedPalette.cardBorder
        )
    }

    CompositionLocalProvider(
        LocalArisePalette provides resolvedPalette,
        LocalVisualStyle provides config.visualStyle,
        LocalLayoutDensity provides config.layoutDensity
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
