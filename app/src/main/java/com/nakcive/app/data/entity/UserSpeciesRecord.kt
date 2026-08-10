package com.nakcive.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "user_species_records",
    foreignKeys = [
        ForeignKey(
            entity = Species::class,
            parentColumns = ["id"],
            childColumns = ["speciesId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
)
data class UserSpeciesRecord(
    @PrimaryKey
    val speciesId: Long,
    val maxSizeCm: Double?,
    val maxWeightKg: Double?,
    val maxRecordId: Long?,
    val firstCaughtAt: Long?,
    val catchCount: Int = 0,
)
