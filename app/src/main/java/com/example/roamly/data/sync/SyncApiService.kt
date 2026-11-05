
package com.example.roamly.data.sync

import com.google.gson.JsonObject
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface SyncApiService {

    @GET("api/health/database")
    suspend fun checkDatabase(): Response<JsonObject>

    @GET("api/holidays")
    suspend fun getHolidays(
        @Header("Authorization") authorization: String
    ): Response<JsonObject>

    @POST("api/holidays")
    suspend fun createHoliday(
        @Header("Authorization") authorization: String,
        @Body holiday: JsonObject
    ): Response<JsonObject>

    @PATCH("api/holidays/{holidayId}")
    suspend fun updateHoliday(
        @Header("Authorization") authorization: String,
        @Path("holidayId") holidayId: String,
        @Body holiday: JsonObject
    ): Response<JsonObject>

    @Multipart
    @POST("api/holidays/{holidayId}/cover")
    suspend fun uploadHolidayCover(
        @Header("Authorization") authorization: String,
        @Path("holidayId") holidayId: String,
        @Part file: MultipartBody.Part
    ): Response<JsonObject>

    @DELETE("api/holidays/{holidayId}")
    suspend fun deleteHoliday(
        @Header("Authorization") authorization: String,
        @Path("holidayId") holidayId: String
    ): Response<JsonObject>

    @GET("api/packing")
    suspend fun getPackingLists(
        @Header("Authorization") authorization: String
    ): Response<JsonObject>

    @POST("api/packing")
    suspend fun createPackingList(
        @Header("Authorization") authorization: String,
        @Body packingList: JsonObject
    ): Response<JsonObject>

    @PATCH("api/packing/{listId}")
    suspend fun updatePackingList(
        @Header("Authorization") authorization: String,
        @Path("listId") listId: String,
        @Body packingList: JsonObject
    ): Response<JsonObject>

    @DELETE("api/packing/{listId}")
    suspend fun deletePackingList(
        @Header("Authorization") authorization: String,
        @Path("listId") listId: String
    ): Response<JsonObject>

    @GET("api/itineraries")
    suspend fun getItineraries(
        @Header("Authorization") authorization: String
    ): Response<JsonObject>

    @GET("api/checklists")
    suspend fun getChecklists(
        @Header("Authorization") authorization: String
    ): Response<JsonObject>
}
