package com.nakcive.app.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nakcive.app.data.DEFAULT_REGION_TAG
import com.nakcive.app.data.NakciveDatabase
import com.nakcive.app.data.api.TideInfo
import com.nakcive.app.data.api.degreesToCompass
import com.nakcive.app.data.entity.FishingRecord
import com.nakcive.app.data.entity.RecordDetail
import com.nakcive.app.data.entity.Species
import com.nakcive.app.data.entity.UserSpeciesRecord
import com.nakcive.app.data.regionTagFromLocation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WeatherSnapshot(
    val waterTempC: Double?,
    val airTempC: Double?,
    val windDirectionDeg: Double?,
    val windSpeedMs: Double?,
    val waveHeightM: Double?,
    val sourceLabel: String,
)

data class AddRecordUiState(
    val photoPath: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val address: String? = null,
    val weatherSnapshot: WeatherSnapshot? = null,
    val tideInfo: TideInfo? = null,
    val speciesName: String = "",
    val sizeCm: String = "",
    val weightKg: String = "",
    val fishingMethod: String = "",
    val memo: String = "",
    val regionTag: String = DEFAULT_REGION_TAG,
    val isSaving: Boolean = false,
    val saveCompleted: Boolean = false,
)

class AddRecordViewModel(application: Application) : AndroidViewModel(application) {
    private val database = NakciveDatabase.getInstance(application)
    private val fishingRecordDao = database.fishingRecordDao()

    private val _uiState = MutableStateFlow(AddRecordUiState())
    val uiState: StateFlow<AddRecordUiState> = _uiState.asStateFlow()

    fun onSpeciesNameChange(value: String) {
        _uiState.update { it.copy(speciesName = value) }
    }

    fun onSizeCmChange(value: String) {
        _uiState.update { it.copy(sizeCm = value) }
    }

    fun onWeightKgChange(value: String) {
        _uiState.update { it.copy(weightKg = value) }
    }

    fun onFishingMethodChange(value: String) {
        _uiState.update { it.copy(fishingMethod = value) }
    }

    fun onMemoChange(value: String) {
        _uiState.update { it.copy(memo = value) }
    }

    fun setCapturedPhoto(
        path: String,
        latitude: Double,
        longitude: Double,
        address: String?,
        weatherSnapshot: WeatherSnapshot?,
        tideInfo: TideInfo?,
    ) {
        _uiState.update {
            it.copy(
                photoPath = path,
                latitude = latitude,
                longitude = longitude,
                address = address,
                weatherSnapshot = weatherSnapshot,
                tideInfo = tideInfo,
                regionTag = regionTagFromLocation(latitude, longitude),
            )
        }
    }

    fun save() {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }

            val speciesName = state.speciesName.trim()
            val sizeCm = state.sizeCm.toDoubleOrNull()
            val weightKg = state.weightKg.toDoubleOrNull()
            val speciesId = if (speciesName.isNotEmpty()) {
                resolveSpeciesId(speciesName, state.regionTag)
            } else {
                null
            }

            val recordId = fishingRecordDao.insert(
                FishingRecord(
                    photoPath = state.photoPath,
                    latitude = state.latitude,
                    longitude = state.longitude,
                    address = state.address,
                    recordedAt = System.currentTimeMillis(),
                    fishingMethod = state.fishingMethod,
                    tideLevel = state.tideInfo?.levelCm,
                    tidePhase = state.tideInfo?.phase,
                    speciesId = speciesId,
                    customSpeciesName = speciesName.ifEmpty { null },
                    sizeCm = sizeCm,
                    weightKg = weightKg,
                    memo = state.memo.ifBlank { null },
                    regionTag = state.regionTag,
                )
            )

            if (speciesId != null) {
                updateUserSpeciesRecord(speciesId, recordId, sizeCm, weightKg)
            }

            if (state.weatherSnapshot != null || state.tideInfo != null) {
                val weather = state.weatherSnapshot
                database.recordDetailDao().insert(
                    RecordDetail(
                        recordId = recordId,
                        waterTemp = weather?.waterTempC,
                        waveHeight = weather?.waveHeightM,
                        windDir = weather?.windDirectionDeg?.let { degreesToCompass(it) },
                        windSpeed = weather?.windSpeedMs,
                        airTemp = weather?.airTempC,
                        humidity = null,
                        obsStationTide = state.tideInfo?.stationName,
                        obsStationWeather = weather?.sourceLabel,
                        fishingIndexAtRecord = null,
                    )
                )
            }

            _uiState.update { it.copy(isSaving = false, saveCompleted = true) }
        }
    }

    private suspend fun resolveSpeciesId(name: String, region: String): Long {
        val speciesDao = database.speciesDao()
        return speciesDao.getByCommonName(name)?.id ?: speciesDao.insert(
            Species(
                commonName = name,
                scientificName = null,
                family = null,
                order = null,
                description = null,
                ecology = null,
                habitat = null,
                regionDistribution = region,
                minLegalSize = null,
                closedSeasonStart = null,
                closedSeasonEnd = null,
                imagePath = null,
            )
        )
    }

    private suspend fun updateUserSpeciesRecord(
        speciesId: Long,
        recordId: Long,
        sizeCm: Double?,
        weightKg: Double?,
    ) {
        val dao = database.userSpeciesRecordDao()
        val existing = dao.getBySpeciesId(speciesId)
        val isNewBest = sizeCm != null && (existing?.maxSizeCm == null || sizeCm > existing.maxSizeCm)
        dao.upsert(
            UserSpeciesRecord(
                speciesId = speciesId,
                maxSizeCm = if (isNewBest) sizeCm else existing?.maxSizeCm,
                maxWeightKg = if (isNewBest) weightKg else (existing?.maxWeightKg ?: weightKg),
                maxRecordId = if (isNewBest || existing == null) recordId else existing.maxRecordId,
                firstCaughtAt = existing?.firstCaughtAt ?: System.currentTimeMillis(),
                catchCount = (existing?.catchCount ?: 0) + 1,
            )
        )
    }

    fun resetSaveCompleted() {
        _uiState.update { it.copy(saveCompleted = false) }
    }
}
