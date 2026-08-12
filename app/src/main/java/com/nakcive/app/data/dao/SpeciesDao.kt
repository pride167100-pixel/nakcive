package com.nakcive.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nakcive.app.data.entity.Species
import kotlinx.coroutines.flow.Flow

@Dao
interface SpeciesDao {
    @Insert
    suspend fun insert(species: Species): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(species: List<Species>)

    @Query("SELECT * FROM species ORDER BY commonName")
    fun getAll(): Flow<List<Species>>

    @Query("SELECT * FROM species WHERE id = :id")
    suspend fun getById(id: Long): Species?

    @Query("SELECT * FROM species WHERE commonName = :name LIMIT 1")
    suspend fun getByCommonName(name: String): Species?

    @Query("SELECT * FROM species WHERE regionDistribution = :region ORDER BY commonName")
    suspend fun getByRegion(region: String): List<Species>

    @Query("SELECT COUNT(*) FROM species")
    suspend fun count(): Int
}
