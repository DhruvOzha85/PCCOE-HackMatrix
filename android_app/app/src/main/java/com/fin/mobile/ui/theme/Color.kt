package com.fin.mobile.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Core FIN Brand Palette - Canonical Hex values
val FinBgPrimary = Color(0xFFF8F7F3)
val FinSurfaceWhite = Color(0xFFFFFFFF)
val FinSurfaceSubtle = Color(0xFFF3F2EE)

// Primary Brand - Deep Government Green
val FinPrimaryGreen = Color(0xFF005B50)
val FinPrimaryGreenDark = Color(0xFF00483F)
val FinPrimaryGreenLight = Color(0xFFEBF3F1)
val FinPrimaryGreenBorder = Color(0xFFBCDCD6)

// Text Colors
val FinTextPrimary = Color(0xFF10243A) // Dark Navy
val FinTextSecondary = Color(0xFF344054)
val FinTextMuted = Color(0xFF667085)
val FinTextInverse = Color(0xFFFFFFFF)

// Accents
val FinAccentSaffron = Color(0xFFE98A00)
val FinAccentSaffronLight = Color(0xFFFEF6EC)
val FinAccentSaffronBorder = Color(0xFFFADBB0)

val FinAccentBlue = Color(0xFF1264D6)
val FinAccentBlueLight = Color(0xFFEEF4FD)
val FinAccentBlueBorder = Color(0xFFBCD5F8)

// Semantic Status Colors
val FinColorSuccess = Color(0xFF087443)
val FinColorSuccessLight = Color(0xFFECFDF3)
val FinColorSuccessBorder = Color(0xFFA6F4C5)

val FinColorWarning = Color(0xFFC56A00)
val FinColorWarningLight = Color(0xFFFFF9F0)
val FinColorWarningBorder = Color(0xFFFEDF89)

val FinColorError = Color(0xFFD92D20)
val FinColorErrorLight = Color(0xFFFEF3F2)
val FinColorErrorBorder = Color(0xFFFECDCA)

// Borders & Dividers
val FinBorderSubtle = Color(0xFFE4E7EC)
val FinBorderMedium = Color(0xFFD0D5DD)

// Dark Theme Variants (maintaining brand accessibility in dark mode)
val FinDarkBgPrimary = Color(0xFF0D1821)
val FinDarkSurface = Color(0xFF142433)
val FinDarkSurfaceSubtle = Color(0xFF1B2E42)
val FinDarkPrimaryGreen = Color(0xFF26A69A)
val FinDarkPrimaryGreenLight = Color(0xFF123832)
val FinDarkTextPrimary = Color(0xFFF4F6F8)
val FinDarkTextSecondary = Color(0xFFD0D5DD)
val FinDarkTextMuted = Color(0xFF98A2B3)
val FinDarkBorderSubtle = Color(0xFF25384D)
val FinDarkBorderMedium = Color(0xFF3B526B)

/**
 * Custom semantic color holder for FIN Design System
 */
@Immutable
data class FinCustomColors(
    val bgPrimary: Color = FinBgPrimary,
    val surfaceWhite: Color = FinSurfaceWhite,
    val surfaceSubtle: Color = FinSurfaceSubtle,
    val primaryGreen: Color = FinPrimaryGreen,
    val primaryGreenDark: Color = FinPrimaryGreenDark,
    val primaryGreenLight: Color = FinPrimaryGreenLight,
    val primaryGreenBorder: Color = FinPrimaryGreenBorder,
    val textPrimary: Color = FinTextPrimary,
    val textSecondary: Color = FinTextSecondary,
    val textMuted: Color = FinTextMuted,
    val textInverse: Color = FinTextInverse,
    val accentSaffron: Color = FinAccentSaffron,
    val accentSaffronLight: Color = FinAccentSaffronLight,
    val accentSaffronBorder: Color = FinAccentSaffronBorder,
    val accentBlue: Color = FinAccentBlue,
    val accentBlueLight: Color = FinAccentBlueLight,
    val accentBlueBorder: Color = FinAccentBlueBorder,
    val success: Color = FinColorSuccess,
    val successLight: Color = FinColorSuccessLight,
    val successBorder: Color = FinColorSuccessBorder,
    val warning: Color = FinColorWarning,
    val warningLight: Color = FinColorWarningLight,
    val warningBorder: Color = FinColorWarningBorder,
    val error: Color = FinColorError,
    val errorLight: Color = FinColorErrorLight,
    val errorBorder: Color = FinColorErrorBorder,
    val borderSubtle: Color = FinBorderSubtle,
    val borderMedium: Color = FinBorderMedium
)

val LocalFinColors = staticCompositionLocalOf { FinCustomColors() }

val FinLightColorScheme = lightColorScheme(
    primary = FinPrimaryGreen,
    onPrimary = FinTextInverse,
    primaryContainer = FinPrimaryGreenLight,
    onPrimaryContainer = FinPrimaryGreenDark,
    secondary = FinAccentSaffron,
    onSecondary = FinTextInverse,
    secondaryContainer = FinAccentSaffronLight,
    onSecondaryContainer = FinColorWarning,
    tertiary = FinAccentBlue,
    onTertiary = FinTextInverse,
    tertiaryContainer = FinAccentBlueLight,
    onTertiaryContainer = FinAccentBlue,
    background = FinBgPrimary,
    onBackground = FinTextPrimary,
    surface = FinSurfaceWhite,
    onSurface = FinTextPrimary,
    surfaceVariant = FinSurfaceSubtle,
    onSurfaceVariant = FinTextSecondary,
    outline = FinBorderSubtle,
    outlineVariant = FinBorderMedium,
    error = FinColorError,
    onError = FinTextInverse,
    errorContainer = FinColorErrorLight,
    onErrorContainer = FinColorError
)

val FinDarkColorScheme = darkColorScheme(
    primary = FinDarkPrimaryGreen,
    onPrimary = FinDarkBgPrimary,
    primaryContainer = FinDarkPrimaryGreenLight,
    onPrimaryContainer = FinDarkTextPrimary,
    secondary = FinAccentSaffron,
    onSecondary = FinDarkBgPrimary,
    secondaryContainer = FinAccentSaffronLight.copy(alpha = 0.2f),
    onSecondaryContainer = FinAccentSaffron,
    tertiary = FinAccentBlue,
    onTertiary = FinDarkBgPrimary,
    tertiaryContainer = FinAccentBlueLight.copy(alpha = 0.2f),
    onTertiaryContainer = FinDarkTextPrimary,
    background = FinDarkBgPrimary,
    onBackground = FinDarkTextPrimary,
    surface = FinDarkSurface,
    onSurface = FinDarkTextPrimary,
    surfaceVariant = FinDarkSurfaceSubtle,
    onSurfaceVariant = FinDarkTextSecondary,
    outline = FinDarkBorderSubtle,
    outlineVariant = FinDarkBorderMedium,
    error = FinColorError,
    onError = FinDarkBgPrimary,
    errorContainer = FinColorErrorLight.copy(alpha = 0.2f),
    onErrorContainer = FinColorError
)