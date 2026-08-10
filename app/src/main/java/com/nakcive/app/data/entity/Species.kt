package com.nakcive.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "species")
data class Species(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val commonName: String,
    val scientificName: String?,
    val family: String?,
    val order: String?,
    val description: String?,
    val ecology: String?,
    val habitat: String?,
    val regionDistribution: String?,
    val minLegalSize: Double?,
    val closedSeasonStart: String?,
    val closedSeasonEnd: String?,
    val imagePath: String?,
)
