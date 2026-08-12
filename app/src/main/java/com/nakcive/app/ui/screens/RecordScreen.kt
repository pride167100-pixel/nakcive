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
import androidx.compose.material3.MaterialTheme
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
    modifier: Modifier = Modifier,
    viewModel: RecordViewModel = viewModel(),
) {
    val records by viewModel.records.collectAsState()

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "기록", fontSize = 24.sp, fontWeight = FontWeight.Bold)

        if (records.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Text("아직 등록된 기록이 없습니다")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(records) { record ->
                    RecordRow(record)
                }
            }
        }

        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("뒤로가기")
        }
    }
}

@Composable
private fun RecordRow(record: FishingRecord) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(12.dp)) {
            RecordThumbnail(photoPath = record.photoPath)
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(
                    text = "${record.customSpeciesName ?: "어종 미입력"}  (${record.regionTag})",
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "크기: ${record.sizeCm?.let { "${it}cm" } ?: "-"}   " +
                        "무게: ${record.weightKg?.let { "${it}kg" } ?: "-"}"
                )
                Text(text = "낚시법: ${record.fishingMethod.ifBlank { "-" }}")
                if (!record.memo.isNullOrBlank()) {
                    Text(text = "메모: ${record.memo}")
                }
            }
        }
    }
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
