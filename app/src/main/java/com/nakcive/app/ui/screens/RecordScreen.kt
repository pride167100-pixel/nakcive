package com.nakcive.app.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun RecordScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    PlaceholderScreen(title = "기록 (준비 중)", onBack = onBack, modifier = modifier)
}
