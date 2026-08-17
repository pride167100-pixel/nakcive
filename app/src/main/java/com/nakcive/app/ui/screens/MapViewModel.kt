package com.nakcive.app.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nakcive.app.data.NakciveDatabase
import com.nakcive.app.data.PublicRestroomRepository
import com.nakcive.app.data.api.KakaoLocalApi
import com.nakcive.app.data.api.NearbyRestroom
import com.nakcive.app.data.entity.FishingRecord
import com.nakcive.app.ui.camera.reverseGeocode
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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

/** 같은 화장실로 볼 수 있는 거리 오차 허용치 (m). 카카오 검색 결과와 정부 데이터가 겹칠 때 중복 제거용. */
private const val DUPLICATE_DISTANCE_M = 30.0
private const val RESULT_LIMIT = 5
private const val GOV_CANDIDATE_LIMIT = 15

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
            val keywordResults = KakaoLocalApi.fetchNearbyRestrooms(latitude, longitude)
            val govResults = fetchGovRestrooms(latitude, longitude)
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

    /** 카카오 검색에 안 걸리는 공원·소규모 화장실을 보강하기 위해 정부 공식 목록에서 후보를 찾아 좌표를 붙인다. */
    private suspend fun fetchGovRestrooms(latitude: Double, longitude: Double): List<NearbyRestroom> {
        val context = getApplication<Application>()
        val address = reverseGeocode(context, latitude, longitude) ?: return emptyList()
        val candidates = PublicRestroomRepository.findCandidates(context, address, GOV_CANDIDATE_LIMIT)
        if (candidates.isEmpty()) return emptyList()

        return coroutineScope {
            candidates.map { entry ->
                async {
                    val coords = KakaoLocalApi.geocodeAddress(entry.address) ?: return@async null
                    val (lat, lng) = coords
                    NearbyRestroom(
                        id = "gov_${entry.name}_${entry.address}".hashCode().toString(),
                        name = entry.name,
                        latitude = lat,
                        longitude = lng,
                        distanceM = distanceMeters(latitude, longitude, lat, lng).toInt(),
                    )
                }
            }.awaitAll().filterNotNull()
        }
    }

    private fun mergeRestrooms(primary: List<NearbyRestroom>, extra: List<NearbyRestroom>): List<NearbyRestroom> {
        val merged = primary.toMutableList()
        extra.forEach { candidate ->
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
