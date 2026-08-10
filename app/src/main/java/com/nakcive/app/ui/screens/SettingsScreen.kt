package com.nakcive.app.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun SettingsScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    PlaceholderScreen(title = "설정 (준비 중)", onBack = onBack, modifier = modifier)
}
