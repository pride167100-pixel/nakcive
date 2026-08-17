package com.nakcive.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.nakcive.app.data.dao.FishingRecordDao
import com.nakcive.app.data.dao.RecordDetailDao
import com.nakcive.app.data.dao.SpeciesDao
import com.nakcive.app.data.dao.TagDao
import com.nakcive.app.data.dao.UserSpeciesRecordDao
import com.nakcive.app.data.entity.FishingRecord
import com.nakcive.app.data.entity.RecordDetail
import com.nakcive.app.data.entity.Species
import com.nakcive.app.data.entity.Tag
import com.nakcive.app.data.entity.UserSpeciesRecord

@Database(
    entities = [
        FishingRecord::class,
        RecordDetail::class,
        Species::class,
        UserSpeciesRecord::class,
        Tag::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class NakciveDatabase : RoomDatabase() {
    abstract fun fishingRecordDao(): FishingRecordDao
    abstract fun recordDetailDao(): RecordDetailDao
    abstract fun speciesDao(): SpeciesDao
    abstract fun userSpeciesRecordDao(): UserSpeciesRecordDao
    abstract fun tagDao(): TagDao

    companion object {
        @Volatile
        private var INSTANCE: NakciveDatabase? = null

        fun getInstance(context: Context): NakciveDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    NakciveDatabase::class.java,
                    "nakcive.db",
                ).build().also { INSTANCE = it }
            }
        }
    }
}
