
package com.example.roamly.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        OfflineHolidayEntity::class,
        OfflineTravelRecordEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class RoamlyOfflineDatabase : RoomDatabase() {

    abstract fun holidayDao(): OfflineHolidayDao

    abstract fun travelRecordDao(): OfflineTravelRecordDao

    companion object {

        @Volatile
        private var instance: RoamlyOfflineDatabase? = null

        fun getInstance(
            context: Context
        ): RoamlyOfflineDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    RoamlyOfflineDatabase::class.java,
                    "roamly_offline.db"
                ).build().also {
                    instance = it
                }
            }
        }
    }
}
