package com.nakcive.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.nakcive.app.data.entity.RecordDetail

@Dao
interface RecordDetailDao {
    @Insert
    suspend fun insert(detail: RecordDetail)

    @Update
    suspend fun update(detail: RecordDetail)

    @Query("SELECT * FROM record_details WHERE recordId = :recordId")
    suspend fun getByRecordId(recordId: Long): RecordDetail?
}
