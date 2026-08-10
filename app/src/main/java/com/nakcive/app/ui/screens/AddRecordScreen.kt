package com.nakcive.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun AddRecordScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddRecordViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.saveCompleted) {
        if (uiState.saveCompleted) {
            viewModel.resetSaveCompleted()
            onBack()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = "＋ 등록", fontSize = 24.sp, fontWeight = FontWeight.Bold)

        OutlinedTextField(
            value = uiState.speciesName,
            onValueChange = viewModel::onSpeciesNameChange,
            label = { Text("어종") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = uiState.sizeCm,
            onValueChange = viewModel::onSizeCmChange,
            label = { Text("크기 (cm)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = uiState.weightKg,
            onValueChange = viewModel::onWeightKgChange,
            label = { Text("무게 (kg)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = uiState.fishingMethod,
            onValueChange = viewModel::onFishingMethodChange,
            label = { Text("낚시법") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = uiState.memo,
            onValueChange = viewModel::onMemoChange,
            label = { Text("메모") },
            modifier = Modifier.fillMaxWidth(),
        )

        Button(
            onClick = viewModel::save,
            enabled = !uiState.isSaving,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (uiState.isSaving) "저장 중..." else "저장")
        }
        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("취소")
        }
    }
}
