package com.nakcive.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage

@Composable
fun RecordDetailScreen(
    recordId: Long,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RecordDetailViewModel = viewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(recordId) {
        viewModel.load(recordId)
    }

    LaunchedEffect(uiState.deleted) {
        if (uiState.deleted) onBack()
    }

    val record = uiState.record
    if (record == null) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
        ) {
            Text(if (uiState.isLoading) "불러오는 중..." else "기록을 찾을 수 없습니다")
            TextButton(onClick = onBack) { Text("뒤로가기") }
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = record.customSpeciesName ?: "어종 미입력",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
        )

        if (record.photoPath.isNotBlank()) {
            AsyncImage(
                model = record.photoPath,
                contentDescription = "기록 사진",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop,
            )
        }

        Text(text = "대표정보", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
        Text(
            text = "위치: ${record.address ?: "주소 확인 불가 (탭하면 지도로 보기)"}",
            textDecoration = TextDecoration.Underline,
            modifier = Modifier.clickable {
                val label = record.customSpeciesName ?: "낚시 기록"
                val uri = Uri.parse(
                    "geo:${record.latitude},${record.longitude}" +
                        "?q=${record.latitude},${record.longitude}($label)",
                )
                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
            },
        )
        Text(text = "낚시법: ${record.fishingMethod.ifBlank { "-" }}")
        Text(text = "물때: ${record.tidePhase ?: "미확인"}")
        Text(
            text = "크기: ${record.sizeCm?.let { "${it}cm" } ?: "-"}   " +
                "무게: ${record.weightKg?.let { "${it}kg" } ?: "-"}",
        )
        if (!record.memo.isNullOrBlank()) {
            Text(text = "메모: ${record.memo}")
        }

        Text(text = "세부정보", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
        val detail = uiState.detail
        Text(text = "수온: ${detail?.waterTemp?.let { "${it}°C" } ?: "미확인"}")
        Text(text = "풍향: ${detail?.windDir ?: "미확인"}")
        Text(text = "풍속: ${detail?.windSpeed?.let { "${it}m/s" } ?: "미확인"}")
        Text(text = "파고: ${detail?.waveHeight?.let { "${it}m" } ?: "미확인"}")

        Button(
            onClick = { showDeleteDialog = true },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB3261E)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
        ) {
            Text("삭제")
        }
        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("뒤로가기")
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("삭제하시겠습니까?") },
            text = { Text("되돌릴 수 없습니다.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.delete()
                }) {
                    Text("삭제")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("취소")
                }
            },
        )
    }
}
