
package com.example.roamly.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface OfflineTravelRecordDao {

    @Query(
        """
        SELECT * FROM offline_travel_records
        WHERE userId = :userId
        AND recordType = :recordType
        ORDER BY updatedAtMillis DESC
        """
    )
    suspend fun getRecords(
        userId: String,
        recordType: String
    ): List<OfflineTravelRecordEntity>

    @Query(
        """
        SELECT * FROM offline_travel_records
        WHERE userId = :userId
        AND recordType = :recordType
        ORDER BY updatedAtMillis DESC
        """
    )
    fun observeRecords(
        userId: String,
        recordType: String
    ): Flow<List<OfflineTravelRecordEntity>>

    @Query(
        """
        SELECT * FROM offline_travel_records
        WHERE userId = :userId
        AND recordType = :recordType
        AND recordId = :recordId
        LIMIT 1
        """
    )
    suspend fun getRecordById(
        userId: String,
        recordType: String,
        recordId: String
    ): OfflineTravelRecordEntity?

    @Upsert
    suspend fun saveRecord(
        record: OfflineTravelRecordEntity
    )

    @Query(
        """
        DELETE FROM offline_travel_records
        WHERE userId = :userId
        AND recordType = :recordType
        AND recordId = :recordId
        """
    )
    suspend fun deleteRecord(
        userId: String,
        recordType: String,
        recordId: String
    ): Int

    @Query(
        """
        SELECT * FROM offline_travel_records
        WHERE userId = :userId
        AND needsSync = 1
        ORDER BY updatedAtMillis ASC
        """
    )
    suspend fun getPendingRecords(
        userId: String
    ): List<OfflineTravelRecordEntity>
}
