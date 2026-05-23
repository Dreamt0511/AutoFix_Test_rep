package com.pocketagent.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = AgentAccent,
    secondary = AgentSecondary,
    background = AgentBackground,
    surface = AgentSurface,
    onPrimary = AgentOnPrimary,
    onSecondary = AgentOnPrimary,
    onBackground = AgentOnPrimary,
    onSurface = AgentOnPrimary,
    error = AgentError
)

@Composable
fun PocketAgentTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = AgentBackground.toArgb()
            window.navigationBarColor = AgentBackground.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}