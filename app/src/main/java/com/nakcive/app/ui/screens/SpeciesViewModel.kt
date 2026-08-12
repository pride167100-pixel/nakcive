package com.nakcive.app.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nakcive.app.data.NakciveDatabase
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

data class SpeciesUiState(
    val entries: List<SpeciesEntry> = emptyList(),
    val isLoading: Boolean = true,
)

private val SEED_SPECIES_NAMES = listOf(
    "감성돔", "참돔", "돌돔", "벵에돔", "벤자리", "혹돔", "다금바리", "능성어",
    "우럭(조피볼락)", "광어(넙치)", "숭어", "농어", "도다리", "노래미", "볼락", "열기(불볼락)", "쏨뱅이",
    "붕장어", "갯장어", "문어", "주꾸미", "낙지", "갑오징어", "무늬오징어", "살오징어",
    "방어", "부시리", "삼치", "전갱이", "고등어", "전어", "갈치", "병어", "준치",
    "임연수어", "도루묵", "자리돔", "학꽁치", "청어", "쥐치",
)

private fun seedSpecies(): List<Species> = SEED_SPECIES_NAMES.map { name ->
    Species(
        commonName = name,
        scientificName = null,
        family = null,
        order = null,
        description = null,
        ecology = null,
        habitat = null,
        regionDistribution = null,
        minLegalSize = null,
        closedSeasonStart = null,
        closedSeasonEnd = null,
        imagePath = null,
    )
}

class SpeciesViewModel(application: Application) : AndroidViewModel(application) {
    private val database = NakciveDatabase.getInstance(application)

    private val _uiState = MutableStateFlow(SpeciesUiState())
    val uiState: StateFlow<SpeciesUiState> = _uiState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val speciesDao = database.speciesDao()
            val existingNames = speciesDao.getAll().first().map { it.commonName }.toSet()
            val missingSeed = seedSpecies().filter { it.commonName !in existingNames }
            if (missingSeed.isNotEmpty()) {
                speciesDao.insertAll(missingSeed)
            }

            val userSpeciesRecordDao = database.userSpeciesRecordDao()
            val fishingRecordDao = database.fishingRecordDao()

            val entries = speciesDao.getAll().first().map { species ->
                val userRecord = userSpeciesRecordDao.getBySpeciesId(species.id)
                val bestPhotoPath = userRecord?.maxRecordId?.let { fishingRecordDao.getById(it)?.photoPath }
                SpeciesEntry(species, userRecord, bestPhotoPath)
            }

            _uiState.update { it.copy(entries = entries, isLoading = false) }
        }
    }
}
