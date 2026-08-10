package com.nakcive.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.nakcive.app.ui.camera.CameraCaptureScreen
import com.nakcive.app.ui.camera.getCurrentLocation
import kotlinx.coroutines.launch
import java.io.File

private enum class AddRecordStep { PERMISSION, CAMERA, FORM }

private val requiredPermissions = arrayOf(
    Manifest.permission.CAMERA,
    Manifest.permission.ACCESS_FINE_LOCATION,
)

@Composable
fun AddRecordScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddRecordViewModel = viewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    fun hasPermissions() = requiredPermissions.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    var step by remember {
        mutableStateOf(if (hasPermissions()) AddRecordStep.CAMERA else AddRecordStep.PERMISSION)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        if (result.values.all { it }) {
            step = AddRecordStep.CAMERA
        }
    }

    LaunchedEffect(uiState.saveCompleted) {
        if (uiState.saveCompleted) {
            viewModel.resetSaveCompleted()
            onBack()
        }
    }

    when (step) {
        AddRecordStep.PERMISSION -> {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "기록을 남기려면 카메라와 위치 권한이 필요해요",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
                Button(
                    onClick = { permissionLauncher.launch(requiredPermissions) },
                    modifier = Modifier.padding(top = 16.dp),
                ) {
                    Text("권한 허용하기")
                }
                TextButton(onClick = onBack, modifier = Modifier.padding(top = 8.dp)) {
                    Text("취소")
                }
            }
        }

        AddRecordStep.CAMERA -> {
            CameraCaptureScreen(
                modifier = modifier,
                onPhotoCaptured = { photoFile ->
                    coroutineScope.launch {
                        val location = getCurrentLocation(context)
                        viewModel.setCapturedPhoto(
                            path = photoFile.absolutePath,
                            latitude = location?.latitude ?: 0.0,
                            longitude = location?.longitude ?: 0.0,
                        )
                        step = AddRecordStep.FORM
                    }
                },
                onCancel = onBack,
            )
        }

        AddRecordStep.FORM -> {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(text = "＋ 등록", fontSize = 24.sp, fontWeight = FontWeight.Bold)

                if (uiState.photoPath.isNotBlank()) {
                    AsyncImage(
                        model = File(uiState.photoPath),
                        contentDescription = "촬영한 사진",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop,
                    )
                }
                Text(text = "위치가 기록되었습니다", fontSize = 14.sp)

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
    }
}
