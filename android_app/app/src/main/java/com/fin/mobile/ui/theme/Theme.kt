package com.fin.mobile.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Accessor for FIN Theme tokens in Composables
 */
object FinTheme {
    val colors: FinCustomColors
        @Composable
        @ReadOnlyComposable
        get() = LocalFinColors.current

    val shapes: FinCustomShapes
        @Composable
        @ReadOnlyComposable
        get() = LocalFinShapes.current

    val typography: Typography
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.typography
}

@Composable
fun FINTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Set to false by default to strictly preserve the FIN governmental brand palette
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> FinDarkColorScheme
        else -> FinLightColorScheme
    }

    val customColors = if (darkTheme) {
        FinCustomColors(
            bgPrimary = FinDarkBgPrimary,
            surfaceWhite = FinDarkSurface,
            surfaceSubtle = FinDarkSurfaceSubtle,
            primaryGreen = FinDarkPrimaryGreen,
            primaryGreenLight = FinDarkPrimaryGreenLight,
            textPrimary = FinDarkTextPrimary,
            textSecondary = FinDarkTextSecondary,
            textMuted = FinDarkTextMuted,
            borderSubtle = FinDarkBorderSubtle,
            borderMedium = FinDarkBorderMedium
        )
    } else {
        FinCustomColors()
    }

    val customShapes = FinCustomShapes()

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !darkTheme
                insetsController.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalFinColors provides customColors,
        LocalFinShapes provides customShapes
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            shapes = FinMaterialShapes,
            typography = FinTypography,
            content = content
        )
    }
}