package com.nakcive.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
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
        Text(text = "설정", fontSize = 24.sp, fontWeight = FontWeight.Bold)

        Text(text = "앱 정보", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
        Text(text = "낚카이브 v${uiState.appVersion}")

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        Text(text = "데이터 관리", fontWeight = FontWeight.Bold)
        if (showCompletedMessage) {
            Text(text = "모든 기록이 삭제되었습니다.", color = Color(0xFFB3261E))
        }
        Button(
            onClick = { showResetDialog = true },
            enabled = !uiState.isResetting,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB3261E)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (uiState.isResetting) "삭제 중..." else "전체 기록 삭제")
        }

        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("뒤로가기")
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
