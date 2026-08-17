package com.nakcive.app.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nakcive.app.data.NakciveDatabase
import com.nakcive.app.data.api.KakaoLocalApi
import com.nakcive.app.data.api.NearbyRestroom
import com.nakcive.app.data.entity.FishingRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RestroomUiState(
    val visible: Boolean = false,
    val isLoading: Boolean = false,
    val restrooms: List<NearbyRestroom> = emptyList(),
    val selectedId: String? = null,
)

class MapViewModel(application: Application) : AndroidViewModel(application) {
    private val fishingRecordDao = NakciveDatabase.getInstance(application).fishingRecordDao()

    val records: StateFlow<List<FishingRecord>> = fishingRecordDao.getAll()
        .map { records -> records.filter { it.latitude != 0.0 || it.longitude != 0.0 } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _restroomState = MutableStateFlow(RestroomUiState())
    val restroomState: StateFlow<RestroomUiState> = _restroomState.asStateFlow()

    fun toggleRestroomsAtCurrentLocation(latitude: Double?, longitude: Double?) {
        if (_restroomState.value.visible) {
            hideRestrooms()
            return
        }
        if (latitude == null || longitude == null) return
        showRestroomsNear(latitude, longitude)
    }

    fun showRestroomsNear(latitude: Double, longitude: Double) {
        _restroomState.update { it.copy(visible = true, isLoading = true, selectedId = null) }
        viewModelScope.launch {
            val results = KakaoLocalApi.fetchNearbyRestrooms(latitude, longitude)
            _restroomState.update { it.copy(isLoading = false, restrooms = results) }
        }
    }

    fun hideRestrooms() {
        _restroomState.value = RestroomUiState(visible = false)
    }

    fun selectRestroom(id: String) {
        _restroomState.update { it.copy(selectedId = if (it.selectedId == id) null else id) }
    }
}
