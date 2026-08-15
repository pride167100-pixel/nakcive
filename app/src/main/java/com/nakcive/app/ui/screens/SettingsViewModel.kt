package com.nakcive.app.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.nakcive.app.BuildConfig
import com.nakcive.app.data.NakciveDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val appVersion: String = BuildConfig.VERSION_NAME,
    val isResetting: Boolean = false,
    val resetCompleted: Boolean = false,
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val database = NakciveDatabase.getInstance(application)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun resetAllData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isResetting = true) }
            database.withTransaction {
                database.fishingRecordDao().deleteAll()
                database.userSpeciesRecordDao().deleteAll()
            }
            _uiState.update { it.copy(isResetting = false, resetCompleted = true) }
        }
    }

    fun resetCompletedShown() {
        _uiState.update { it.copy(resetCompleted = false) }
    }
}
