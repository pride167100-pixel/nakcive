package com.nakcive.app.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nakcive.app.data.NakciveDatabase
import com.nakcive.app.data.SpeciesRecordSync
import com.nakcive.app.data.SpeciesSeeder
import com.nakcive.app.data.entity.Species
import com.nakcive.app.data.entity.UserSpeciesRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SpeciesEntry(
    val species: Species,
    val userRecord: UserSpeciesRecord?,
    val bestPhotoPath: String?,
)

enum class SpeciesSortMode {
    DEFAULT, CATCH_COUNT, MAX_SIZE, MAX_WEIGHT
}

data class SpeciesUiState(
    val entries: List<SpeciesEntry> = emptyList(),
    val isLoading: Boolean = true,
    val sortMode: SpeciesSortMode = SpeciesSortMode.DEFAULT,
)

private fun sortSpeciesEntries(entries: List<SpeciesEntry>, mode: SpeciesSortMode): List<SpeciesEntry> {
    return when (mode) {
        SpeciesSortMode.DEFAULT -> entries.sortedWith(
            compareByDescending<SpeciesEntry> { (it.userRecord?.catchCount ?: 0) > 0 }
                .thenBy { it.species.commonName }
        )
        SpeciesSortMode.CATCH_COUNT -> entries.sortedWith(
            compareByDescending<SpeciesEntry> { it.userRecord?.catchCount ?: 0 }
                .thenBy { it.species.commonName }
        )
        SpeciesSortMode.MAX_SIZE -> entries.sortedWith(
            compareByDescending<SpeciesEntry> { it.userRecord?.maxSizeCm ?: -1.0 }
                .thenBy { it.species.commonName }
        )
        SpeciesSortMode.MAX_WEIGHT -> entries.sortedWith(
            compareByDescending<SpeciesEntry> { it.userRecord?.maxWeightKg ?: -1.0 }
                .thenBy { it.species.commonName }
        )
    }
}

class SpeciesViewModel(application: Application) : AndroidViewModel(application) {
    private val database = NakciveDatabase.getInstance(application)

    private val _uiState = MutableStateFlow(SpeciesUiState())
    val uiState: StateFlow<SpeciesUiState> = _uiState.asStateFlow()

    private var rawEntries: List<SpeciesEntry> = emptyList()

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val speciesDao = database.speciesDao()
            SpeciesSeeder.ensureSeeded(speciesDao)

            val userSpeciesRecordDao = database.userSpeciesRecordDao()
            val fishingRecordDao = database.fishingRecordDao()

            // 삭제 등으로 낡아진 도감 기록(예: 이미 지워진 기록을 가리키는 항목)을
            // 화면을 그리기 전에 실제 기록 기준으로 다시 계산해 스스로 치유한다.
            val existingRecordSpeciesIds = userSpeciesRecordDao.getAll().first().map { it.speciesId }
            existingRecordSpeciesIds.forEach { speciesId ->
                SpeciesRecordSync.recalculate(database, speciesId)
            }

            rawEntries = speciesDao.getAll().first().map { species ->
                val userRecord = userSpeciesRecordDao.getBySpeciesId(species.id)
                val bestPhotoPath = userRecord?.maxRecordId?.let { fishingRecordDao.getById(it)?.photoPath }
                SpeciesEntry(species, userRecord, bestPhotoPath)
            }

            _uiState.update {
                it.copy(entries = sortSpeciesEntries(rawEntries, it.sortMode), isLoading = false)
            }
        }
    }

    fun setSortMode(mode: SpeciesSortMode) {
        _uiState.update { it.copy(entries = sortSpeciesEntries(rawEntries, mode), sortMode = mode) }
    }
}
