
package com.example.roamly.data.local

import android.content.Context
import androidx.room.withTransaction
import com.example.roamly.data.auth.SessionManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class RoomTravelStore(
    context: Context
) {
    private val applicationContext =
        context.applicationContext

    private val userId =
        SessionManager(context).getUserId()
            ?.takeIf { it.isNotBlank() && it != "guest" }
            ?: throw IllegalStateException(
                "Sign in to access offline travel data."
            )

    private val database =
        RoamlyOfflineDatabase.getInstance(
            applicationContext
        )

    private val dao = database.travelRecordDao()

    private val gson = Gson()

    suspend fun <T : Any> read(
        recordType: String,
        preferenceName: String,
        preferenceKey: String,
        modelClass: Class<T>,
        idOf: (T) -> String,
        prefix: String? = null
    ): MutableList<T> {
        migrate(
            recordType,
            preferenceName,
            preferenceKey,
            modelClass,
            idOf,
            prefix
        )

        return dao.getRecords(userId, recordType)
            .filter {
                prefix == null ||
                        it.recordId.startsWith(prefix)
            }
            .map {
                gson.fromJson(
                    it.dataJson,
                    modelClass
                )
            }
            .toMutableList()
    }

    suspend fun <T : Any> replace(
        recordType: String,
        preferenceName: String,
        preferenceKey: String,
        modelClass: Class<T>,
        records: List<T>,
        idOf: (T) -> String,
        prefix: String? = null
    ) {
        migrate(
            recordType,
            preferenceName,
            preferenceKey,
            modelClass,
            idOf,
            prefix
        )

        replaceInternal(
            recordType,
            records,
            idOf,
            prefix
        )
    }

    private suspend fun <T : Any> migrate(
        recordType: String,
        preferenceName: String,
        preferenceKey: String,
        modelClass: Class<T>,
        idOf: (T) -> String,
        prefix: String?
    ) {
        val preferences =
            applicationContext.getSharedPreferences(
                preferenceName,
                Context.MODE_PRIVATE
            )

        val migrationKey =
            "room_v1_${recordType}_${preferenceKey}"

        if (preferences.getBoolean(migrationKey, false)) {
            return
        }

        migrationMutex.withLock {
            if (preferences.getBoolean(migrationKey, false)) {
                return@withLock
            }

            val legacyJson =
                preferences.getString(
                    preferenceKey,
                    null
                )

            if (legacyJson != null) {
                val listType =
                    TypeToken.getParameterized(
                        List::class.java,
                        modelClass
                    ).type

                val legacyRecords =
                    gson.fromJson<List<T>>(
                        legacyJson,
                        listType
                    ) ?: throw IllegalStateException(
                        "Unable to migrate $recordType."
                    )

                replaceInternal(
                    recordType,
                    legacyRecords,
                    idOf,
                    prefix
                )
            }

            check(
                preferences.edit()
                    .putBoolean(migrationKey, true)
                    .commit()
            ) {
                "Unable to record $recordType migration."
            }
        }
    }

    private suspend fun <T : Any> replaceInternal(
        recordType: String,
        records: List<T>,
        idOf: (T) -> String,
        prefix: String?
    ) {
        database.withTransaction {
            val existing =
                dao.getRecords(
                    userId,
                    recordType
                ).filter {
                    prefix == null ||
                            it.recordId.startsWith(prefix)
                }.associateBy {
                    it.recordId
                }

            val current =
                records.associateBy {
                    idOf(it)
                }

            for ((recordId, record) in current) {
                require(
                    prefix == null ||
                            recordId.startsWith(prefix)
                )

                val json = gson.toJson(record)
                val previous = existing[recordId]

                if (
                    previous == null ||
                    previous.dataJson != json
                ) {
                    dao.saveRecord(
                        OfflineTravelRecordEntity(
                            userId = userId,
                            recordType = recordType,
                            recordId = recordId,
                            dataJson = json,
                            updatedAtMillis =
                                System.currentTimeMillis(),
                            needsSync = true
                        )
                    )
                }

                dao.deleteRecord(
                    userId,
                    "${recordType}_deletion",
                    recordId
                )
            }

            for (previous in existing.values) {
                if (previous.recordId !in current) {
                    dao.saveRecord(
                        OfflineTravelRecordEntity(
                            userId = userId,
                            recordType =
                                "${recordType}_deletion",
                            recordId =
                                previous.recordId,
                            dataJson = gson.toJson(
                                mapOf(
                                    "recordId" to
                                            previous.recordId
                                )
                            ),
                            updatedAtMillis =
                                System.currentTimeMillis(),
                            needsSync = true
                        )
                    )

                    dao.deleteRecord(
                        userId,
                        recordType,
                        previous.recordId
                    )
                }
            }
        }
    }

    companion object {
        private val migrationMutex = Mutex()
    }
}
