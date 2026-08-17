package com.nakcive.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.nakcive.app.data.dao.FishingRecordDao
import com.nakcive.app.data.dao.GeocodedRestroomDao
import com.nakcive.app.data.dao.RecordDetailDao
import com.nakcive.app.data.dao.SpeciesDao
import com.nakcive.app.data.dao.TagDao
import com.nakcive.app.data.dao.UserSpeciesRecordDao
import com.nakcive.app.data.entity.FishingRecord
import com.nakcive.app.data.entity.GeocodedRestroom
import com.nakcive.app.data.entity.RecordDetail
import com.nakcive.app.data.entity.Species
import com.nakcive.app.data.entity.Tag
import com.nakcive.app.data.entity.UserSpeciesRecord

private val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `geocoded_restrooms` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `address` TEXT NOT NULL,
                `latitude` REAL NOT NULL,
                `longitude` REAL NOT NULL
            )
            """.trimIndent(),
        )
    }
}

@Database(
    entities = [
        FishingRecord::class,
        RecordDetail::class,
        Species::class,
        UserSpeciesRecord::class,
        Tag::class,
        GeocodedRestroom::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class NakciveDatabase : RoomDatabase() {
    abstract fun fishingRecordDao(): FishingRecordDao
    abstract fun recordDetailDao(): RecordDetailDao
    abstract fun speciesDao(): SpeciesDao
    abstract fun userSpeciesRecordDao(): UserSpeciesRecordDao
    abstract fun tagDao(): TagDao
    abstract fun geocodedRestroomDao(): GeocodedRestroomDao

    companion object {
        @Volatile
        private var INSTANCE: NakciveDatabase? = null

        fun getInstance(context: Context): NakciveDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    NakciveDatabase::class.java,
                    "nakcive.db",
                ).addMigrations(MIGRATION_2_3).build().also { INSTANCE = it }
            }
        }
    }
}
