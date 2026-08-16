package com.nakcive.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nakcive.app.ui.theme.NakciveTopBar

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var showResetDialog by remember { mutableStateOf(false) }
    var showCompletedMessage by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.resetCompleted) {
        if (uiState.resetCompleted) {
            showCompletedMessage = true
            viewModel.resetCompletedShown()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        NakciveTopBar(title = "설정", onBack = onBack)

        SettingsSectionCard(title = "앱 정보") {
            Text(text = "낚카이브 v${uiState.appVersion}", fontSize = 14.sp)
        }

        SettingsSectionCard(title = "데이터 관리") {
            if (showCompletedMessage) {
                Text(
                    text = "모든 기록이 삭제되었습니다.",
                    color = Color(0xFFB3261E),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
            Button(
                onClick = { showResetDialog = true },
                enabled = !uiState.isResetting,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB3261E)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (uiState.isResetting) "삭제 중..." else "전체 기록 삭제")
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("모든 기록을 삭제하시겠습니까?") },
            text = { Text("등록된 낚시 기록과 도감 통계가 전부 삭제됩니다. 되돌릴 수 없습니다.") },
            confirmButton = {
                TextButton(onClick = {
                    showResetDialog = false
                    showCompletedMessage = false
                    viewModel.resetAllData()
                }) {
                    Text("삭제")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("취소")
                }
            },
        )
    }
}

@Composable
private fun SettingsSectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
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
