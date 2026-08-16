package com.nakcive.app.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.nakcive.app.BuildConfig
import com.nakcive.app.data.NakciveDatabase
import com.nakcive.app.data.ThemeMode
import com.nakcive.app.data.ThemePreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val appVersion: String = BuildConfig.VERSION_NAME,
    val isResetting: Boolean = false,
    val resetCompleted: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val database = NakciveDatabase.getInstance(application)

    private val _uiState = MutableStateFlow(SettingsUiState(themeMode = ThemePreferences.themeMode.value))
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            ThemePreferences.themeMode.collect { mode ->
                _uiState.update { it.copy(themeMode = mode) }
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        ThemePreferences.setThemeMode(getApplication(), mode)
    }

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
