package com.nakcive.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
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
import androidx.compose.material3.MaterialTheme
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
import com.nakcive.app.data.ObservationStationRepository
import com.nakcive.app.data.api.KmaForecastApi
import com.nakcive.app.data.api.KmaGrid
import com.nakcive.app.data.api.MarineDataApi
import com.nakcive.app.data.api.TideApi
import com.nakcive.app.ui.camera.CameraCaptureScreen
import com.nakcive.app.ui.camera.getCurrentLocation
import com.nakcive.app.ui.camera.reverseGeocode
import com.nakcive.app.ui.theme.NakciveTopBar
import kotlinx.coroutines.launch

private enum class AddRecordStep { PERMISSION, CAMERA, FORM }

private val requiredPermissions = buildList {
    add(Manifest.permission.CAMERA)
    add(Manifest.permission.ACCESS_FINE_LOCATION)
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }
}.toTypedArray()

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
                onPhotoCaptured = { photoUri ->
                    coroutineScope.launch {
                        val location = getCurrentLocation(context)
                        val latitude = location?.latitude ?: 0.0
                        val longitude = location?.longitude ?: 0.0
                        val address = if (location != null) {
                            reverseGeocode(context, latitude, longitude)
                        } else {
                            null
                        }
                        val buoyStation = if (location != null) {
                            ObservationStationRepository.findNearest(context, latitude, longitude, "TW_")
                        } else {
                            null
                        }
                        val buoyObservation = buoyStation?.let {
                            MarineDataApi.fetchLatestBuoyObservation(it.code)
                        }

                        val needsForecast = location != null && (
                            buoyObservation == null ||
                                buoyObservation.windDirectionDeg == null ||
                                buoyObservation.windSpeedMs == null ||
                                buoyObservation.waveHeightM == null ||
                                buoyObservation.airTempC == null
                            )
                        val forecast = if (needsForecast) {
                            val (nx, ny) = KmaGrid.latLonToGrid(latitude, longitude)
                            KmaForecastApi.fetchNearestForecast(nx, ny)
                        } else {
                            null
                        }

                        val weatherSnapshot = if (buoyObservation != null || forecast != null) {
                            WeatherSnapshot(
                                waterTempC = buoyObservation?.waterTempC,
                                airTempC = buoyObservation?.airTempC ?: forecast?.airTempC,
                                windDirectionDeg = buoyObservation?.windDirectionDeg ?: forecast?.windDirectionDeg,
                                windSpeedMs = buoyObservation?.windSpeedMs ?: forecast?.windSpeedMs,
                                waveHeightM = buoyObservation?.waveHeightM ?: forecast?.waveHeightM,
                                sourceLabel = buildString {
                                    if (buoyObservation != null) append(buoyObservation.stationName)
                                    if (forecast != null) {
                                        if (isNotEmpty()) append(" · ")
                                        append("기상청 예보")
                                    }
                                },
                            )
                        } else {
                            null
                        }

                        val tideStation = if (location != null) {
                            ObservationStationRepository.findNearest(context, latitude, longitude, "DT_")
                        } else {
                            null
                        }
                        val tideInfo = tideStation?.let { TideApi.fetchTideInfo(it.code) }

                        viewModel.setCapturedPhoto(
                            path = photoUri.toString(),
                            latitude = latitude,
                            longitude = longitude,
                            address = address,
                            weatherSnapshot = weatherSnapshot,
                            tideInfo = tideInfo,
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
                NakciveTopBar(title = "등록", onBack = onBack)

                if (uiState.photoPath.isNotBlank()) {
                    AsyncImage(
                        model = uiState.photoPath,
                        contentDescription = "촬영한 사진",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop,
                    )
                }
                Text(
                    text = uiState.address?.let { "위치: $it" }
                        ?: "위치가 기록되었습니다 (지역: ${uiState.regionTag})",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                uiState.weatherSnapshot?.let { weather ->
                    Text(
                        text = "수온 ${weather.waterTempC?.let { "${it}°C" } ?: "-"} · " +
                            "파고 ${weather.waveHeightM?.let { "${it}m" } ?: "-"} " +
                            "(${weather.sourceLabel})",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                uiState.tideInfo?.phase?.let { phase ->
                    Text(
                        text = "물때: $phase",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

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
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                ) {
                    Text(if (uiState.isSaving) "저장 중..." else "저장")
                }
            }
        }
    }
}
