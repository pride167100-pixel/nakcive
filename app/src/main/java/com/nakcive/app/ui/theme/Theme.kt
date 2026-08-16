package com.nakcive.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import com.nakcive.app.data.ThemeMode
import com.nakcive.app.data.ThemePreferences

// 바다·낚시 느낌의 청록-딥블루 계열 팔레트. 런처 아이콘 배경(#0B6E8C)과 통일.
private val OceanBlue = Color(0xFF0B6E8C)
private val OceanBlueLight = Color(0xFFB3E0EC)
private val SunsetCoral = Color(0xFFFF7A59)
private val SeafoamTeal = Color(0xFF26A69A)
private val WarmSand = Color(0xFFFDF8F2)

private val NakciveLightColorScheme = lightColorScheme(
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

private val OceanBlueDark = Color(0xFF7EC8E3)
private val SunsetCoralDark = Color(0xFFFF9B80)
private val SeafoamTealDark = Color(0xFF5FC1B6)

private val NakciveDarkColorScheme = darkColorScheme(
    primary = OceanBlueDark,
    onPrimary = Color(0xFF00344A),
    primaryContainer = Color(0xFF0B4A5F),
    onPrimaryContainer = Color(0xFFCFEBF5),
    secondary = SunsetCoralDark,
    onSecondary = Color(0xFF4A1B0E),
    tertiary = SeafoamTealDark,
    onTertiary = Color(0xFF0A2D29),
    background = Color(0xFF14171A),
    onBackground = Color(0xFFE9E7E3),
    surface = Color(0xFF1C2023),
    onSurface = Color(0xFFE9E7E3),
    surfaceVariant = Color(0xFF2A3236),
    onSurfaceVariant = Color(0xFFC2CBCE),
    outline = Color(0xFF8C9598),
)

@Composable
fun NakciveTheme(content: @Composable () -> Unit) {
    val themeMode by ThemePreferences.themeMode.collectAsState()
    val useDarkTheme = themeMode == ThemeMode.DARK
    MaterialTheme(
        colorScheme = if (useDarkTheme) NakciveDarkColorScheme else NakciveLightColorScheme,
        content = content,
    )
}
