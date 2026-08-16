package com.nakcive.app.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nakcive.app.data.NakciveDatabase
import com.nakcive.app.data.entity.FishingRecord
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class MapViewModel(application: Application) : AndroidViewModel(application) {
    private val fishingRecordDao = NakciveDatabase.getInstance(application).fishingRecordDao()

    val records: StateFlow<List<FishingRecord>> = fishingRecordDao.getAll()
        .map { records -> records.filter { it.latitude != 0.0 || it.longitude != 0.0 } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
