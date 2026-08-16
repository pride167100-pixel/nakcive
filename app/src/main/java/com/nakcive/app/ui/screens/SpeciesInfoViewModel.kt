package com.nakcive.app.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nakcive.app.data.NakciveDatabase
import com.nakcive.app.data.SpeciesSeeder
import com.nakcive.app.data.entity.Species
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SpeciesInfoUiState(
    val species: List<Species> = emptyList(),
    val isLoading: Boolean = true,
    val searchQuery: String = "",
)

class SpeciesInfoViewModel(application: Application) : AndroidViewModel(application) {
    private val database = NakciveDatabase.getInstance(application)

    private val _uiState = MutableStateFlow(SpeciesInfoUiState())
    val uiState: StateFlow<SpeciesInfoUiState> = _uiState.asStateFlow()

    private var rawSpecies: List<Species> = emptyList()

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val speciesDao = database.speciesDao()
            SpeciesSeeder.ensureSeeded(speciesDao)
            rawSpecies = speciesDao.getAll().first()
            _uiState.update {
                it.copy(species = applyFilter(it.searchQuery), isLoading = false)
            }
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(species = applyFilter(query), searchQuery = query) }
    }

    private fun applyFilter(query: String): List<Species> {
        return if (query.isBlank()) {
            rawSpecies
        } else {
            rawSpecies.filter {
                it.commonName.contains(query, ignoreCase = true) ||
                    (it.scientificName?.contains(query, ignoreCase = true) == true)
            }
        }
    }
}
