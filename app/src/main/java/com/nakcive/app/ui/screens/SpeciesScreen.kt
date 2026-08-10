package com.nakcive.app.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun SpeciesScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    PlaceholderScreen(title = "도감 (준비 중)", onBack = onBack, modifier = modifier)
}
