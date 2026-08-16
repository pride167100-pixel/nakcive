package com.nakcive.app.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nakcive.app.data.NakciveDatabase
import com.nakcive.app.data.entity.FishingRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

enum class RecordSortMode { NEWEST, OLDEST, SIZE, WEIGHT }

data class RecordUiState(
    val records: List<FishingRecord> = emptyList(),
    val searchQuery: String = "",
    val sortMode: RecordSortMode = RecordSortMode.NEWEST,
)

class RecordViewModel(application: Application) : AndroidViewModel(application) {
    private val fishingRecordDao = NakciveDatabase.getInstance(application).fishingRecordDao()

    private val searchQuery = MutableStateFlow("")
    private val sortMode = MutableStateFlow(RecordSortMode.NEWEST)

    val uiState: StateFlow<RecordUiState> = combine(
        fishingRecordDao.getAll(),
        searchQuery,
        sortMode,
    ) { records, query, mode ->
        val filtered = if (query.isBlank()) {
            records
        } else {
            records.filter { record ->
                record.customSpeciesName?.contains(query, ignoreCase = true) == true ||
                    record.address?.contains(query, ignoreCase = true) == true
            }
        }
        val sorted = when (mode) {
            RecordSortMode.NEWEST -> filtered.sortedByDescending { it.recordedAt }
            RecordSortMode.OLDEST -> filtered.sortedBy { it.recordedAt }
            RecordSortMode.SIZE -> filtered.sortedByDescending { it.sizeCm ?: -1.0 }
            RecordSortMode.WEIGHT -> filtered.sortedByDescending { it.weightKg ?: -1.0 }
        }
        RecordUiState(records = sorted, searchQuery = query, sortMode = mode)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RecordUiState())

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun setSortMode(mode: RecordSortMode) {
        sortMode.value = mode
    }
}
