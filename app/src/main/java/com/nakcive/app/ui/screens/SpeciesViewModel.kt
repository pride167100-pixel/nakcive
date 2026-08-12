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

private fun seedSpecies(): List<Species> = listOf(
    "우럭" to "서해", "광어" to "서해", "숭어" to "서해", "농어" to "서해", "주꾸미" to "서해",
    "감성돔" to "남해", "벵에돔" to "남해", "참돔" to "남해", "돌돔" to "남해", "볼락" to "남해",
    "오징어" to "동해", "방어" to "동해", "열기" to "동해", "도루묵" to "동해", "임연수어" to "동해",
    "자리돔" to "제주", "벤자리" to "제주", "다금바리" to "제주", "혹돔" to "제주",
).map { (name, region) ->
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
}

class SpeciesViewModel(application: Application) : AndroidViewModel(application) {
    private val database = NakciveDatabase.getInstance(application)

    private val _uiState = MutableStateFlow(SpeciesUiState())
    val uiState: StateFlow<SpeciesUiState> = _uiState.asStateFlow()

    fun loadRegion(region: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val speciesDao = database.speciesDao()
            if (speciesDao.count() == 0) {
                speciesDao.insertAll(seedSpecies())
            }

            val userSpeciesRecordDao = database.userSpeciesRecordDao()
            val fishingRecordDao = database.fishingRecordDao()

            val entries = speciesDao.getByRegion(region).map { species ->
                val userRecord = userSpeciesRecordDao.getBySpeciesId(species.id)
                val bestPhotoPath = userRecord?.maxRecordId?.let { fishingRecordDao.getById(it)?.photoPath }
                SpeciesEntry(species, userRecord, bestPhotoPath)
            }

            _uiState.update { it.copy(entries = entries, isLoading = false) }
        }
    }
}
