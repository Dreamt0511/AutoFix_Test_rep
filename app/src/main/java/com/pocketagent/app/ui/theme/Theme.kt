package com.pocketagent.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Ollama 风格配色方案
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1A73E8),  // 深蓝色
    secondary = Color(0xFF5F6368), // 灰色
    tertiary = Color(0xFF34A853),  // 绿色
    background = Color(0xFFF8F9FA), // 浅灰背景
    surface = Color(0xFFFFFFFF),   // 白色表面
    surfaceVariant = Color(0xFFF1F3F4), // 浅灰表面变体
    onPrimary = Color(0xFFFFFFFF),
    onSecondary = Color(0xFFFFFFFF),
    onBackground = Color(0xFF202124), // 深灰文字
    onSurface = Color(0xFF202124),
    error = Color(0xFFD93025),
    errorContainer = Color(0xFFFCE8E6),
    outline = Color(0xFFDADCE0), // 边框颜色
    primaryContainer = Color(0xFFE8F0FE), // 浅蓝色容器
    secondaryContainer = Color(0xFFF1F3F4)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF8AB4F8),  // 浅蓝色
    secondary = Color(0xFF9AA0A6), // 浅灰色
    tertiary = Color(0xFF81C995),  // 浅绿色
    background = Color(0xFF202124), // 深灰背景
    surface = Color(0xFF292A2D),   // 深灰表面
    surfaceVariant = Color(0xFF3C4043), // 深灰表面变体
    onPrimary = Color(0xFF202124),
    onSecondary = Color(0xFF202124),
    onBackground = Color(0xFFE8EAED), // 浅灰文字
    onSurface = Color(0xFFE8EAED),
    error = Color(0xFFF28B82),
    errorContainer = Color(0xFF3C2B2A),
    outline = Color(0xFF5F6368),
    primaryContainer = Color(0xFF2D3C5A),
    secondaryContainer = Color(0xFF3C4043)
)

@Composable
fun PocketAgentTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}