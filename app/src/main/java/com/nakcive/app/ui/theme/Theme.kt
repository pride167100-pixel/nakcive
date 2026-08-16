package com.nakcive.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// 바다·낚시 느낌의 청록-딥블루 계열 팔레트. 런처 아이콘 배경(#0B6E8C)과 통일.
private val OceanBlue = Color(0xFF0B6E8C)
private val OceanBlueLight = Color(0xFFB3E0EC)
private val SunsetCoral = Color(0xFFFF7A59)
private val SeafoamTeal = Color(0xFF26A69A)
private val WarmSand = Color(0xFFFDF8F2)

private val NakciveColorScheme = lightColorScheme(
    primary = OceanBlue,
    onPrimary = Color.White,
    primaryContainer = OceanBlueLight,
    onPrimaryContainer = Color(0xFF00344A),
    secondary = SunsetCoral,
    onSecondary = Color.White,
    tertiary = SeafoamTeal,
    onTertiary = Color.White,
    background = WarmSand,
    onBackground = Color(0xFF201B16),
    surface = Color.White,
    onSurface = Color(0xFF201B16),
    surfaceVariant = Color(0xFFE3EEF1),
    onSurfaceVariant = Color(0xFF3F4A4D),
    outline = Color(0xFF6F7A7D),
)

@Composable
fun NakciveTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NakciveColorScheme,
        content = content,
    )
}
