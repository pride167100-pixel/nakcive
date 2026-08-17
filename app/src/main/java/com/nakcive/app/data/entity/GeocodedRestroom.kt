package com.nakcive.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** 정부 공식 화장실 목록(PublicRestroomRepository)의 주소를 한 번 좌표로 변환해 캐시해둔 것. */
@Entity(tableName = "geocoded_restrooms")
data class GeocodedRestroom(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
)
