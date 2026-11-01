
package com.example.roamly.data.sync

import com.google.gson.JsonObject
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Url

interface TravelSyncApi {

    @GET
    suspend fun get(
        @Url url: String,
        @Header("Authorization") authorization: String
    ): Response<JsonObject>

    @POST
    suspend fun post(
        @Url url: String,
        @Header("Authorization") authorization: String,
        @Body body: JsonObject
    ): Response<JsonObject>

    @PATCH
    suspend fun patch(
        @Url url: String,
        @Header("Authorization") authorization: String,
        @Body body: JsonObject
    ): Response<JsonObject>

    @DELETE
    suspend fun delete(
        @Url url: String,
        @Header("Authorization") authorization: String
    ): Response<JsonObject>
}
