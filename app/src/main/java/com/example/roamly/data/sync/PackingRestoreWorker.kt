
package com.example.roamly.data.sync

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.roamly.data.auth.SessionManager
import com.example.roamly.data.local.OfflineTravelRecordEntity
import com.example.roamly.data.local.RoamlyOfflineDatabase
import com.example.roamly.data.model.PackingItem
import com.example.roamly.data.model.PackingList
import com.example.roamly.data.network.ApiConfig
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.CancellationException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class PackingRestoreWorker(
    context: Context,
    parameters: WorkerParameters
) : CoroutineWorker(context, parameters) {

    private val database =
        RoamlyOfflineDatabase.getInstance(applicationContext)

    private val dao = database.travelRecordDao()
    private val gson = Gson()
    private val sessions = SessionManager(applicationContext)

    private val api = Retrofit.Builder()
        .baseUrl(ApiConfig.BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(SyncApiService::class.java)

    override suspend fun doWork(): Result {
        val session = sessions.getSession()
            ?: return Result.success()

        val userId = session.userId

        return try {
            val pending = dao.getPendingRecords(userId).any {
                it.recordType in setOf(
                    "packing_list",
                    "packing_item",
                    "packing_list_deletion",
                    "packing_item_deletion"
                )
            }

            if (pending) return Result.retry()

            val response =
                api.getPackingLists("Bearer ${session.accessToken}")

            check(response.isSuccessful) {
                "Packing download HTTP ${response.code()}"
            }

            val data = response.body()
                ?.getAsJsonArray("packingLists")
                ?: error("Azure packingLists array missing")

            val listMappings =
                dao.getRecords(
                    userId,
                    "sync_mapping_packing_list"
                ).mapNotNull { record ->
                    val id = gson.fromJson(
                        record.dataJson,
                        JsonObject::class.java
                    ).get("serverId")?.asString

                    if (id == null) null
                    else id to record.recordId
                }.toMap()

            val itemMappings =
                dao.getRecords(
                    userId,
                    "sync_mapping_packing_item"
                ).mapNotNull { record ->
                    val id = gson.fromJson(
                        record.dataJson,
                        JsonObject::class.java
                    ).get("serverId")?.asString

                    if (id == null) null
                    else id to record.recordId
                }.toMap()

            for (entry in data) {
                if (sessions.getSession()?.userId != userId) {
                    return Result.success()
                }

                val remote = entry.asJsonObject
                val remoteListId =
                    remote.get("packingListId").asString

                val localListId =
                    listMappings[remoteListId] ?: remoteListId

                val deleted = dao.getRecordById(
                    userId,
                    "packing_list_deletion",
                    localListId
                )

                if (deleted != null) continue

                val existing = dao.getRecordById(
                    userId,
                    "packing_list",
                    localListId
                )

                database.withTransaction {
                    if (existing == null) {
                        val list = gson.fromJson(
                            remote,
                            PackingList::class.java
                        ).copy(packingListId = localListId)

                        dao.saveRecord(
                            OfflineTravelRecordEntity(
                                userId = userId,
                                recordType = "packing_list",
                                recordId = localListId,
                                dataJson = gson.toJson(list),
                                needsSync = false
                            )
                        )
                    }

                    putMapping(
                        userId,
                        "packing_list",
                        localListId,
                        remoteListId
                    )
                }

                val items =
                    remote.getAsJsonArray("items") ?: continue

                for (itemEntry in items) {
                    val item = itemEntry.asJsonObject

                    val remoteItemId =
                        item.get("packingItemId").asString

                    val localItemId =
                        itemMappings[remoteItemId] ?: remoteItemId

                    if (
                        dao.getRecordById(
                            userId,
                            "packing_item_deletion",
                            localItemId
                        ) != null
                    ) {
                        continue
                    }

                    database.withTransaction {
                        if (
                            dao.getRecordById(
                                userId,
                                "packing_item",
                                localItemId
                            ) == null
                        ) {
                            val restored = gson.fromJson(
                                item,
                                PackingItem::class.java
                            ).copy(
                                packingItemId = localItemId,
                                packingListId = localListId
                            )

                            dao.saveRecord(
                                OfflineTravelRecordEntity(
                                    userId = userId,
                                    recordType = "packing_item",
                                    recordId = localItemId,
                                    dataJson = gson.toJson(restored),
                                    needsSync = false
                                )
                            )
                        }

                        putMapping(
                            userId,
                            "packing_item",
                            localItemId,
                            remoteItemId
                        )
                    }
                }
            }

            Result.success()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Log.e(
                "RoamlyPackingSync",
                "Packing restoration failed",
                error
            )

            Result.retry()
        }
    }

    private suspend fun putMapping(
        userId: String,
        type: String,
        localId: String,
        serverId: String
    ) {
        val json = JsonObject().apply {
            addProperty("serverId", serverId)
        }

        dao.saveRecord(
            OfflineTravelRecordEntity(
                userId = userId,
                recordType = "sync_mapping_$type",
                recordId = localId,
                dataJson = json.toString(),
                needsSync = false
            )
        )
    }

    companion object {
        fun request(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val work =
                OneTimeWorkRequestBuilder<PackingRestoreWorker>()
                    .setConstraints(constraints)
                    .build()

            WorkManager.getInstance(context.applicationContext)
                .enqueueUniqueWork(
                    "roamly_packing_restore",
                    ExistingWorkPolicy.APPEND_OR_REPLACE,
                    work
                )
        }
    }
}
