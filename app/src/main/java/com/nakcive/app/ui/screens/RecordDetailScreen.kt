package com.nakcive.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.nakcive.app.share.ShareCardGenerator
import com.nakcive.app.ui.theme.NakciveTopBar
import kotlinx.coroutines.launch

@Composable
fun RecordDetailScreen(
    recordId: Long,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RecordDetailViewModel = viewModel(),
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isSharing by remember { mutableStateOf(false) }

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
            NakciveTopBar(title = "기록 상세", onBack = onBack)
            Text(if (uiState.isLoading) "불러오는 중..." else "기록을 찾을 수 없습니다")
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
        NakciveTopBar(title = record.customSpeciesName ?: "어종 미입력", onBack = onBack)

        if (record.photoPath.isNotBlank()) {
            AsyncImage(
                model = record.photoPath,
                contentDescription = "기록 사진",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop,
            )
        }

        InfoSectionCard(title = "대표정보") {
            InfoLine(
                label = "위치",
                value = record.address ?: "주소 확인 불가 (탭하면 지도로 보기)",
                valueUnderline = true,
                onValueClick = {
                    val label = record.customSpeciesName ?: "낚시 기록"
                    val uri = Uri.parse(
                        "geo:${record.latitude},${record.longitude}" +
                            "?q=${record.latitude},${record.longitude}($label)",
                    )
                    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                },
            )
            InfoLine(label = "낚시법", value = record.fishingMethod.ifBlank { "-" })
            InfoLine(label = "물때", value = record.tidePhase ?: "미확인")
            InfoLine(
                label = "크기/무게",
                value = "${record.sizeCm?.let { "${it}cm" } ?: "-"}   " +
                    "${record.weightKg?.let { "${it}kg" } ?: "-"}",
            )
            if (!record.memo.isNullOrBlank()) {
                InfoLine(label = "메모", value = record.memo)
            }
        }

        val detail = uiState.detail
        InfoSectionCard(title = "세부정보") {
            InfoLine(label = "수온", value = detail?.waterTemp?.let { "${it}°C" } ?: "미확인")
            InfoLine(label = "기온", value = detail?.airTemp?.let { "${it}°C" } ?: "미확인")
            InfoLine(label = "풍향", value = detail?.windDir ?: "미확인")
            InfoLine(label = "풍속", value = detail?.windSpeed?.let { "${it}m/s" } ?: "미확인")
            InfoLine(label = "파고", value = detail?.waveHeight?.let { "${it}m" } ?: "미확인")
            if (!detail?.obsStationWeather.isNullOrBlank()) {
                Text(
                    text = "관측소: ${detail?.obsStationWeather}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }

        Button(
            enabled = !isSharing,
            onClick = {
                isSharing = true
                coroutineScope.launch {
                    val speciesLabel = record.customSpeciesName ?: "어종 미입력"
                    val uri = ShareCardGenerator.generate(context, record, speciesLabel)
                    isSharing = false
                    if (uri != null) {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "image/jpeg"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "공유하기"))
                    } else {
                        Toast.makeText(context, "카드 생성에 실패했습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
        ) {
            Text(if (isSharing) "카드 만드는 중..." else "공유하기")
        }
        Button(
            onClick = onEdit,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        ) {
            Text("수정")
        }
        Button(
            onClick = { showDeleteDialog = true },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB3261E)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 16.dp),
        ) {
            Text("삭제")
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

@Composable
private fun InfoSectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.primary,
            )
            content()
        }
    }
}

@Composable
private fun InfoLine(
    label: String,
    value: String,
    valueUnderline: Boolean = false,
    onValueClick: (() -> Unit)? = null,
) {
    Row(
        modifier = if (onValueClick != null) Modifier.clickable(onClick = onValueClick) else Modifier,
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(end = 8.dp),
        )
        Text(
            text = value,
            fontSize = 14.sp,
            textDecoration = if (valueUnderline) TextDecoration.Underline else null,
        )
    }
}
