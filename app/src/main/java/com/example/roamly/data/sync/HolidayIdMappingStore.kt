
package com.example.roamly.data.sync

import android.content.Context
import com.example.roamly.data.auth.SessionManager
import com.example.roamly.data.local.OfflineTravelRecordEntity
import com.example.roamly.data.local.RoamlyOfflineDatabase
import com.google.gson.JsonObject
import java.util.UUID

class HolidayIdMappingStore(
    context: Context
) {

    private val userId =
        SessionManager(context).getUserId()
            ?.takeIf {
                it.isNotBlank() && it != "guest"
            }
            ?: throw IllegalStateException(
                "An authenticated user is required."
            )

    private val dao =
        RoamlyOfflineDatabase
            .getInstance(context.applicationContext)
            .travelRecordDao()

    suspend fun getServerId(
        localHolidayId: Int
    ): String? {
        val record = dao.getRecordById(
            userId,
            RECORD_TYPE,
            localHolidayId.toString()
        ) ?: return null

        val data = try {
            com.google.gson.JsonParser.parseString(
                record.dataJson
            ).asJsonObject
        } catch (_: Exception) {
            throw IllegalStateException(
                "Holiday ID mapping is invalid."
            )
        }

        return data.get("serverHolidayId")
            ?.takeUnless { it.isJsonNull }
            ?.asString
    }

    suspend fun saveMapping(
        localHolidayId: Int,
        serverHolidayId: String
    ) {
        require(localHolidayId > 0)

        val validatedId =
            UUID.fromString(serverHolidayId)
                .toString()

        val existing = getServerId(localHolidayId)

        check(
            existing == null ||
                    existing == validatedId
        ) {
            "This holiday already has a different server ID."
        }

        val data = JsonObject().apply {
            addProperty(
                "localHolidayId",
                localHolidayId
            )
            addProperty(
                "serverHolidayId",
                validatedId
            )
        }

        dao.saveRecord(
            OfflineTravelRecordEntity(
                userId = userId,
                recordType = RECORD_TYPE,
                recordId = localHolidayId.toString(),
                dataJson = data.toString(),
                updatedAtMillis =
                    System.currentTimeMillis(),
                needsSync = false
            )
        )
    }

    companion object {
        private const val RECORD_TYPE =
            "holiday_server_mapping"
    }
}
