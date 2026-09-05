package com.gemofgemma.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val OmniCodeColorScheme = lightColorScheme(
    primary = GemPrimaryLight,
    onPrimary = GemOnPrimaryLight,
    primaryContainer = GemPrimaryContainerLight,
    onPrimaryContainer = GemOnPrimaryContainerLight,
    secondary = GemSecondaryLight,
    onSecondary = GemOnSecondaryLight,
    secondaryContainer = GemSecondaryContainerLight,
    onSecondaryContainer = GemOnSecondaryContainerLight,
    tertiary = GemTertiaryLight,
    onTertiary = GemOnTertiaryLight,
    tertiaryContainer = GemTertiaryContainerLight,
    onTertiaryContainer = GemOnTertiaryContainerLight,
    background = GemBackgroundLight,
    onBackground = GemOnBackgroundLight,
    surface = GemSurfaceLight,
    onSurface = GemOnSurfaceLight,
    surfaceVariant = GemSurfaceVariantLight,
    onSurfaceVariant = GemOnSurfaceVariantLight,
    outline = GemOutlineLight,
    surfaceContainer = GemSurfaceContainerLight,
    surfaceContainerHigh = GemSurfaceContainerHighLight,
    error = GemErrorLight,
    onError = GemOnErrorLight
)

@Composable
fun OmniCodeTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = true
                isAppearanceLightNavigationBars = true
            }
        }
    }

    MaterialTheme(
        colorScheme = OmniCodeColorScheme,
        typography = GemTypography,
        shapes = GemShapes,
        content = content
    )
}
