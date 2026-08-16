package com.nakcive.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import com.nakcive.app.ui.theme.NakciveTopBar

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
        NakciveTopBar(title = "도감", onBack = onBack)

        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = viewModel::setSearchQuery,
            label = { Text("어종명 검색") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SortModeChip(
                label = "기본",
                selected = uiState.sortMode == SpeciesSortMode.DEFAULT,
                onClick = { viewModel.setSortMode(SpeciesSortMode.DEFAULT) },
            )
            SortModeChip(
                label = "마릿수순",
                selected = uiState.sortMode == SpeciesSortMode.CATCH_COUNT,
                onClick = { viewModel.setSortMode(SpeciesSortMode.CATCH_COUNT) },
            )
            SortModeChip(
                label = "최장순",
                selected = uiState.sortMode == SpeciesSortMode.MAX_SIZE,
                onClick = { viewModel.setSortMode(SpeciesSortMode.MAX_SIZE) },
            )
            SortModeChip(
                label = "최고무게순",
                selected = uiState.sortMode == SpeciesSortMode.MAX_WEIGHT,
                onClick = { viewModel.setSortMode(SpeciesSortMode.MAX_WEIGHT) },
            )
        }

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
    }
}

@Composable
private fun SortModeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
    )
}

@Composable
private fun SpeciesRow(entry: SpeciesEntry) {
    val caught = (entry.userRecord?.catchCount ?: 0) > 0
    val thumbnailModifier = Modifier
        .size(64.dp)
        .clip(RoundedCornerShape(8.dp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (caught) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (caught) 2.dp else 0.dp),
    ) {
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
                    fontSize = 16.sp,
                    color = if (caught) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                )
                if (caught) {
                    val record = entry.userRecord!!
                    Text(
                        text = "최고 기록 ${record.maxSizeCm?.let { "${it}cm" } ?: "-"}  " +
                            "${record.maxWeightKg?.let { "${it}kg" } ?: ""}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "잡은 횟수 ${record.catchCount}회",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(text = "미등록", fontSize = 13.sp, color = MaterialTheme.colorScheme.outline)
                }
            }
        }
    }
}
