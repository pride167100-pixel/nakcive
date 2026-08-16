package com.nakcive.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.nakcive.app.data.entity.FishingRecord

@Composable
fun RecordScreen(
    onBack: () -> Unit,
    onRecordClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RecordViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "기록", fontSize = 24.sp, fontWeight = FontWeight.Bold)

        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = viewModel::setSearchQuery,
            label = { Text("어종명 또는 주소 검색") },
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
            RecordSortChip("최신순", uiState.sortMode == RecordSortMode.NEWEST) {
                viewModel.setSortMode(RecordSortMode.NEWEST)
            }
            RecordSortChip("오래된순", uiState.sortMode == RecordSortMode.OLDEST) {
                viewModel.setSortMode(RecordSortMode.OLDEST)
            }
            RecordSortChip("크기순", uiState.sortMode == RecordSortMode.SIZE) {
                viewModel.setSortMode(RecordSortMode.SIZE)
            }
            RecordSortChip("무게순", uiState.sortMode == RecordSortMode.WEIGHT) {
                viewModel.setSortMode(RecordSortMode.WEIGHT)
            }
        }

        if (uiState.records.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (uiState.searchQuery.isBlank()) {
                        "아직 등록된 기록이 없습니다"
                    } else {
                        "검색 결과가 없습니다"
                    },
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(uiState.records) { record ->
                    RecordRow(record, onClick = { onRecordClick(record.id) })
                }
            }
        }

        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("뒤로가기")
        }
    }
}

@Composable
private fun RecordSortChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
    )
}

@Composable
private fun RecordRow(record: FishingRecord, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(modifier = Modifier.padding(12.dp)) {
            RecordThumbnail(photoPath = record.photoPath)
            Column(modifier = Modifier.padding(start = 12.dp)) {
                val locationLabel = extractLocalityLabel(record.address, record.regionTag)
                Text(
                    text = "${record.customSpeciesName ?: "어종 미입력"}  ($locationLabel)",
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "크기: ${record.sizeCm?.let { "${it}cm" } ?: "-"}   " +
                        "무게: ${record.weightKg?.let { "${it}kg" } ?: "-"}"
                )
                Text(text = "낚시법: ${record.fishingMethod.ifBlank { "-" }}")
                Text(text = "위치: ${record.address ?: "-"}")
                if (!record.memo.isNullOrBlank()) {
                    Text(text = "메모: ${record.memo}")
                }
            }
        }
    }
}

/** 상세 주소(지번 포함)에서 '읍/면/동/리/가'로 끝나는 지명 토큰만 골라 제목 옆에 짧게 표시한다. */
private fun extractLocalityLabel(address: String?, fallback: String): String {
    if (address.isNullOrBlank()) return fallback
    val localitySuffixes = listOf("읍", "면", "동", "리", "가")
    val tokens = address.trim().split(" ").filter { it.isNotBlank() }
    return tokens.lastOrNull { token -> localitySuffixes.any { token.endsWith(it) } }
        ?: tokens.lastOrNull()
        ?: fallback
}

@Composable
private fun RecordThumbnail(photoPath: String) {
    val thumbnailModifier = Modifier
        .size(64.dp)
        .clip(RoundedCornerShape(8.dp))

    if (photoPath.isNotBlank()) {
        AsyncImage(
            model = photoPath,
            contentDescription = "기록 사진",
            modifier = thumbnailModifier,
            contentScale = ContentScale.Crop,
        )
    } else {
        Box(
            modifier = thumbnailModifier.background(MaterialTheme.colorScheme.surfaceVariant),
        )
    }
}
