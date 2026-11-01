
package com.example.roamly.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface OfflineHolidayDao {

    @Query(
        """
        SELECT * FROM offline_holidays
        WHERE userId = :userId
        ORDER BY startDate ASC, holidayId ASC
        """
    )
    suspend fun getHolidays(
        userId: String
    ): List<OfflineHolidayEntity>

    @Query(
        """
        SELECT * FROM offline_holidays
        WHERE userId = :userId
        ORDER BY startDate ASC, holidayId ASC
        """
    )
    fun observeHolidays(
        userId: String
    ): Flow<List<OfflineHolidayEntity>>

    @Query(
        """
        SELECT * FROM offline_holidays
        WHERE userId = :userId
        AND holidayId = :holidayId
        LIMIT 1
        """
    )
    suspend fun getHolidayById(
        userId: String,
        holidayId: Int
    ): OfflineHolidayEntity?

    @Upsert
    suspend fun saveHoliday(
        holiday: OfflineHolidayEntity
    )

    @Query(
        """
        DELETE FROM offline_holidays
        WHERE userId = :userId
        AND holidayId = :holidayId
        """
    )
    suspend fun deleteHoliday(
        userId: String,
        holidayId: Int
    ): Int

    @Query(
        """
        SELECT COUNT(*) FROM offline_holidays
        WHERE userId = :userId
        """
    )
    suspend fun getHolidayCount(
        userId: String
    ): Int
}
