package com.nakcive.app.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun MapScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    PlaceholderScreen(title = "지도 (준비 중)", onBack = onBack, modifier = modifier)
}
