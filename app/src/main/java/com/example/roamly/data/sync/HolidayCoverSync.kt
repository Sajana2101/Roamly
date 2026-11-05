
package com.example.roamly.data.sync

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import com.example.roamly.data.auth.SessionManager
import com.example.roamly.data.local.RoamlyOfflineDatabase
import com.example.roamly.data.model.Holiday
import com.example.roamly.data.network.ApiConfig
import com.google.gson.Gson
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class HolidayCoverSync(context: Context) {

    private val appContext = context.applicationContext
    private val sessions = SessionManager(appContext)
    private val database =
        RoamlyOfflineDatabase.getInstance(appContext)
    private val holidays = database.holidayDao()
    private val gson = Gson()

    private val api = Retrofit.Builder()
        .baseUrl(ApiConfig.BASE_URL)
        .addConverterFactory(
            GsonConverterFactory.create()
        )
        .build()
        .create(SyncApiService::class.java)

    suspend fun uploadPending(): Boolean {
        val session = sessions.getSession() ?: return true
        val userId = session.userId
        val auth = "Bearer ${session.accessToken}"
        val mappings = HolidayIdMappingStore(appContext)

        var success = true

        for (snapshot in holidays.getHolidays(userId)) {
            if (
                sessions.getSession()?.userId != userId
            ) {
                return false
            }

            try {
                val current = holidays.getHolidayById(
                    userId,
                    snapshot.holidayId
                ) ?: continue

                if (current.needsSync) continue

                val holiday = gson.fromJson(
                    current.holidayJson,
                    Holiday::class.java
                )

                val source =
                    holiday.coverImageUri ?: continue

                if (
                    !source.startsWith("content://", true) &&
                    !source.startsWith("file://", true)
                ) {
                    continue
                }

                val serverId = mappings.getServerId(
                    current.holidayId
                ) ?: continue

                val bytes = withContext(Dispatchers.IO) {
                    HolidayCoverImage.jpegBytes(
                        appContext,
                        source
                    )
                }

                val part = MultipartBody.Part.createFormData(
                    "file",
                    "cover.jpg",
                    bytes.toRequestBody(
                        "image/jpeg".toMediaType()
                    )
                )

                val response = api.uploadHolidayCover(
                    auth,
                    serverId,
                    part
                )

                if (!response.isSuccessful) {
                    error(
                        "Cover upload failed with HTTP ${response.code()}"
                    )
                }

                val body = response.body()
                    ?: error("Empty cover response")

                val imageUrl = body.get("imageUrl")
                    ?.takeUnless { it.isJsonNull }
                    ?.asString
                    ?.takeIf {
                        it.startsWith("https://", true)
                    }
                    ?: error(
                        "Cover upload did not return an HTTPS URL"
                    )

                withContext(Dispatchers.IO) {
                    HolidayCoverImage.saveUploaded(
                        appContext,
                        userId,
                        imageUrl,
                        bytes
                    )
                }

                database.withTransaction {
                    val latest = holidays.getHolidayById(
                        userId,
                        current.holidayId
                    )

                    if (
                        latest != null &&
                        latest.holidayJson == current.holidayJson &&
                        latest.updatedAtMillis ==
                        current.updatedAtMillis
                    ) {
                        holidays.saveHoliday(
                            latest.copy(
                                holidayJson = gson.toJson(
                                    holiday.copy(
                                        coverImageUri = imageUrl
                                    )
                                ),
                                needsSync = false
                            )
                        )
                    }
                }

                Log.i(
                    "HolidayCoverSync",
                    "Uploaded cover for ${current.holidayId}"
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                success = false

                Log.e(
                    "HolidayCoverSync",
                    "Cover upload failed for ${snapshot.holidayId}",
                    error
                )
            }
        }

        return success
    }
}
