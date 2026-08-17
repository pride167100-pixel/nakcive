package com.nakcive.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.nakcive.app.data.entity.GeocodedRestroom

@Dao
interface GeocodedRestroomDao {
    @Insert
    suspend fun insertAll(entries: List<GeocodedRestroom>)

    @Query("SELECT COUNT(*) FROM geocoded_restrooms")
    suspend fun count(): Int

    @Query("SELECT * FROM geocoded_restrooms")
    suspend fun getAll(): List<GeocodedRestroom>
}
