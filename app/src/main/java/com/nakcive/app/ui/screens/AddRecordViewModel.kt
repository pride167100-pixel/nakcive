package com.nakcive.app.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nakcive.app.data.DEFAULT_REGION_TAG
import com.nakcive.app.data.NakciveDatabase
import com.nakcive.app.data.entity.FishingRecord
import com.nakcive.app.data.regionTagFromLocation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddRecordUiState(
    val photoPath: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
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
    private val fishingRecordDao = NakciveDatabase.getInstance(application).fishingRecordDao()

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

    fun setCapturedPhoto(path: String, latitude: Double, longitude: Double) {
        _uiState.update {
            it.copy(
                photoPath = path,
                latitude = latitude,
                longitude = longitude,
                regionTag = regionTagFromLocation(latitude, longitude),
            )
        }
    }

    fun save() {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            fishingRecordDao.insert(
                FishingRecord(
                    photoPath = state.photoPath,
                    latitude = state.latitude,
                    longitude = state.longitude,
                    recordedAt = System.currentTimeMillis(),
                    fishingMethod = state.fishingMethod,
                    tideLevel = null,
                    tidePhase = null,
                    speciesId = null,
                    customSpeciesName = state.speciesName.ifBlank { null },
                    sizeCm = state.sizeCm.toDoubleOrNull(),
                    weightKg = state.weightKg.toDoubleOrNull(),
                    memo = state.memo.ifBlank { null },
                    regionTag = state.regionTag,
                )
            )
            _uiState.update { it.copy(isSaving = false, saveCompleted = true) }
        }
    }

    fun resetSaveCompleted() {
        _uiState.update { it.copy(saveCompleted = false) }
    }
}
