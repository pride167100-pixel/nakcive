package com.nakcive.app.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nakcive.app.data.NakciveDatabase
import com.nakcive.app.data.SpeciesRecordSync
import com.nakcive.app.data.entity.FishingRecord
import com.nakcive.app.data.entity.RecordDetail
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RecordDetailUiState(
    val record: FishingRecord? = null,
    val detail: RecordDetail? = null,
    val isLoading: Boolean = true,
    val deleted: Boolean = false,
)

class RecordDetailViewModel(application: Application) : AndroidViewModel(application) {
    private val database = NakciveDatabase.getInstance(application)

    private val _uiState = MutableStateFlow(RecordDetailUiState())
    val uiState: StateFlow<RecordDetailUiState> = _uiState.asStateFlow()

    fun load(recordId: Long) {
        viewModelScope.launch {
            val record = database.fishingRecordDao().getById(recordId)
            val detail = database.recordDetailDao().getByRecordId(recordId)
            _uiState.update { it.copy(record = record, detail = detail, isLoading = false) }
        }
    }

    fun delete() {
        val record = _uiState.value.record ?: return
        viewModelScope.launch {
            database.fishingRecordDao().delete(record)
            record.speciesId?.let { speciesId -> SpeciesRecordSync.recalculate(database, speciesId) }
            _uiState.update { it.copy(deleted = true) }
        }
    }
}
