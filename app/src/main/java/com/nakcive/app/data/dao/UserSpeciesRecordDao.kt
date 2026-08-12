package com.nakcive.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.nakcive.app.data.entity.UserSpeciesRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface UserSpeciesRecordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: UserSpeciesRecord)

    @Update
    suspend fun update(record: UserSpeciesRecord)

    @Delete
    suspend fun delete(record: UserSpeciesRecord)

    @Query("SELECT * FROM user_species_records WHERE speciesId = :speciesId")
    suspend fun getBySpeciesId(speciesId: Long): UserSpeciesRecord?

    @Query("SELECT * FROM user_species_records")
    fun getAll(): Flow<List<UserSpeciesRecord>>
}
