package com.nakcive.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.nakcive.app.data.entity.FishingRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface FishingRecordDao {
    @Insert
    suspend fun insert(record: FishingRecord): Long

    @Update
    suspend fun update(record: FishingRecord)

    @Delete
    suspend fun delete(record: FishingRecord)

    @Query("SELECT * FROM fishing_records ORDER BY recordedAt DESC")
    fun getAll(): Flow<List<FishingRecord>>

    @Query("SELECT * FROM fishing_records WHERE id = :id")
    suspend fun getById(id: Long): FishingRecord?

    @Query("SELECT * FROM fishing_records WHERE regionTag = :regionTag ORDER BY recordedAt DESC")
    fun getByRegion(regionTag: String): Flow<List<FishingRecord>>

    @Query("SELECT * FROM fishing_records WHERE speciesId = :speciesId ORDER BY recordedAt DESC")
    fun getBySpecies(speciesId: Long): Flow<List<FishingRecord>>

    @Query("DELETE FROM fishing_records")
    suspend fun deleteAll()
}
