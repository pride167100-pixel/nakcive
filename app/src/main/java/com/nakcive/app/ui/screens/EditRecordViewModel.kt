package com.nakcive.app.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nakcive.app.data.DEFAULT_REGION_TAG
import com.nakcive.app.data.NakciveDatabase
import com.nakcive.app.data.SpeciesRecordSync
import com.nakcive.app.data.entity.Species
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EditRecordUiState(
    val recordId: Long = 0,
    val photoPath: String = "",
    val speciesName: String = "",
    val sizeCm: String = "",
    val weightKg: String = "",
    val fishingMethod: String = "",
    val memo: String = "",
    val regionTag: String = DEFAULT_REGION_TAG,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val saveCompleted: Boolean = false,
    val notFound: Boolean = false,
)

class EditRecordViewModel(application: Application) : AndroidViewModel(application) {
    private val database = NakciveDatabase.getInstance(application)

    private val _uiState = MutableStateFlow(EditRecordUiState())
    val uiState: StateFlow<EditRecordUiState> = _uiState.asStateFlow()

    fun load(recordId: Long) {
        viewModelScope.launch {
            val record = database.fishingRecordDao().getById(recordId)
            if (record != null) {
                _uiState.update {
                    it.copy(
                        recordId = record.id,
                        photoPath = record.photoPath,
                        speciesName = record.customSpeciesName ?: "",
                        sizeCm = record.sizeCm?.toString() ?: "",
                        weightKg = record.weightKg?.toString() ?: "",
                        fishingMethod = record.fishingMethod,
                        memo = record.memo ?: "",
                        regionTag = record.regionTag,
                        isLoading = false,
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false, notFound = true) }
            }
        }
    }

    fun onPhotoPathChange(path: String) {
        _uiState.update { it.copy(photoPath = path) }
    }

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

    fun save() {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }

            val existing = database.fishingRecordDao().getById(state.recordId)
            if (existing == null) {
                _uiState.update { it.copy(isSaving = false) }
                return@launch
            }

            val oldSpeciesId = existing.speciesId
            val speciesName = state.speciesName.trim()
            val sizeCm = state.sizeCm.toDoubleOrNull()
            val weightKg = state.weightKg.toDoubleOrNull()
            val newSpeciesId = if (speciesName.isNotEmpty()) {
                resolveSpeciesId(speciesName, state.regionTag)
            } else {
                null
            }

            database.fishingRecordDao().update(
                existing.copy(
                    photoPath = state.photoPath,
                    fishingMethod = state.fishingMethod,
                    speciesId = newSpeciesId,
                    customSpeciesName = speciesName.ifEmpty { null },
                    sizeCm = sizeCm,
                    weightKg = weightKg,
                    memo = state.memo.ifBlank { null },
                    updatedAt = System.currentTimeMillis(),
                )
            )

            oldSpeciesId?.let { SpeciesRecordSync.recalculate(database, it) }
            if (newSpeciesId != null && newSpeciesId != oldSpeciesId) {
                SpeciesRecordSync.recalculate(database, newSpeciesId)
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
}
