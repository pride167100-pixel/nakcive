package com.nakcive.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fishing_records")
data class FishingRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val photoPath: String,
    val latitude: Double,
    val longitude: Double,
    val address: String?,
    val recordedAt: Long,
    val fishingMethod: String,
    val tideLevel: Double?,
    val tidePhase: String?,
    val speciesId: Long?,
    val customSpeciesName: String?,
    val sizeCm: Double?,
    val weightKg: Double?,
    val memo: String?,
    val regionTag: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
