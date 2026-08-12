package com.nakcive.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage

@Composable
fun SpeciesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SpeciesViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.load()
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "도감", fontSize = 24.sp, fontWeight = FontWeight.Bold)

        if (uiState.entries.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Text(if (uiState.isLoading) "불러오는 중..." else "등록된 어종이 없습니다")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(uiState.entries) { entry ->
                    SpeciesRow(entry)
                }
            }
        }

        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("뒤로가기")
        }
    }
}

@Composable
private fun SpeciesRow(entry: SpeciesEntry) {
    val caught = (entry.userRecord?.catchCount ?: 0) > 0
    val thumbnailModifier = Modifier
        .size(64.dp)
        .clip(RoundedCornerShape(8.dp))

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(12.dp)) {
            if (caught && !entry.bestPhotoPath.isNullOrBlank()) {
                AsyncImage(
                    model = entry.bestPhotoPath,
                    contentDescription = entry.species.commonName,
                    modifier = thumbnailModifier,
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(modifier = thumbnailModifier.background(MaterialTheme.colorScheme.surfaceVariant))
            }
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(
                    text = entry.species.commonName,
                    fontWeight = FontWeight.Bold,
                    color = if (caught) LocalContentColor.current else MaterialTheme.colorScheme.outline,
                )
                if (caught) {
                    val record = entry.userRecord!!
                    Text(
                        text = "최고 기록: ${record.maxSizeCm?.let { "${it}cm" } ?: "-"}  " +
                            "${record.maxWeightKg?.let { "${it}kg" } ?: ""}",
                    )
                    Text(text = "잡은 횟수: ${record.catchCount}회")
                } else {
                    Text(text = "미등록", color = MaterialTheme.colorScheme.outline)
                }
            }
        }
    }
}
