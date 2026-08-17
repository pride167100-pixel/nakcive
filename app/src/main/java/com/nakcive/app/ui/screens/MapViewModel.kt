package com.nakcive.app.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nakcive.app.data.NakciveDatabase
import com.nakcive.app.data.RestroomGeocodeSync
import com.nakcive.app.data.api.KakaoLocalApi
import com.nakcive.app.data.api.NearbyRestroom
import com.nakcive.app.data.entity.FishingRecord
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
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
    val isPreparing: Boolean = false,
    val prepDone: Int = 0,
    val prepTotal: Int = 0,
    val restrooms: List<NearbyRestroom> = emptyList(),
    val selectedId: String? = null,
)

/** 같은 화장실로 볼 수 있는 거리 오차 허용치 (m). 카카오 검색 결과와 정부 데이터가 겹칠 때 중복 제거용. */
private const val DUPLICATE_DISTANCE_M = 30.0
private const val RESULT_LIMIT = 5

class MapViewModel(application: Application) : AndroidViewModel(application) {
    private val database = NakciveDatabase.getInstance(application)
    private val fishingRecordDao = database.fishingRecordDao()

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
        _restroomState.update { it.copy(visible = true, selectedId = null) }
        viewModelScope.launch {
            val dao = database.geocodedRestroomDao()
            if (dao.count() == 0) {
                _restroomState.update { it.copy(isPreparing = true, isLoading = true) }
                RestroomGeocodeSync.ensureReady(getApplication(), database) { done, total ->
                    _restroomState.update { it.copy(prepDone = done, prepTotal = total) }
                }
                _restroomState.update { it.copy(isPreparing = false) }
            }

            _restroomState.update { it.copy(isLoading = true) }
            val keywordResults = KakaoLocalApi.fetchNearbyRestrooms(latitude, longitude)
            val govResults = dao.getAll().map { entry ->
                NearbyRestroom(
                    id = "gov_${entry.id}",
                    name = entry.name,
                    latitude = entry.latitude,
                    longitude = entry.longitude,
                    distanceM = distanceMeters(latitude, longitude, entry.latitude, entry.longitude).toInt(),
                )
            }
            val merged = mergeRestrooms(keywordResults, govResults)
            _restroomState.update { it.copy(isLoading = false, restrooms = merged) }
        }
    }

    fun hideRestrooms() {
        _restroomState.value = RestroomUiState(visible = false)
    }

    fun selectRestroom(id: String) {
        _restroomState.update { it.copy(selectedId = if (it.selectedId == id) null else id) }
    }

    private fun mergeRestrooms(primary: List<NearbyRestroom>, extra: List<NearbyRestroom>): List<NearbyRestroom> {
        val merged = primary.toMutableList()
        extra.sortedBy { it.distanceM ?: Int.MAX_VALUE }.forEach { candidate ->
            val isDuplicate = merged.any { existing ->
                distanceMeters(existing.latitude, existing.longitude, candidate.latitude, candidate.longitude) <
                    DUPLICATE_DISTANCE_M
            }
            if (!isDuplicate) merged.add(candidate)
        }
        return merged.sortedBy { it.distanceM ?: Int.MAX_VALUE }.take(RESULT_LIMIT)
    }

    private fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadiusM = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadiusM * c
    }
}
