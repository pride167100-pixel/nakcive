package com.nakcive.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "record_details",
    foreignKeys = [
        ForeignKey(
            entity = FishingRecord::class,
            parentColumns = ["id"],
            childColumns = ["recordId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
)
data class RecordDetail(
    @PrimaryKey
    val recordId: Long,
    val waterTemp: Double?,
    val waveHeight: Double?,
    val windDir: String?,
    val windSpeed: Double?,
    val airTemp: Double?,
    val humidity: Double?,
    val obsStationTide: String?,
    val obsStationWeather: String?,
    val fishingIndexAtRecord: Int?,
)
