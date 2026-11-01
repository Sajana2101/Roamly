
package com.example.roamly.data.sync

import com.google.gson.JsonObject
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
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

interface DocumentSyncApi {

    @GET("api/documents")
    suspend fun list(
        @Header("Authorization") auth: String
    ): Response<JsonObject>

    @Multipart
    @POST("api/documents/upload")
    suspend fun upload(
        @Header("Authorization") auth: String,
        @Part file: MultipartBody.Part,
        @Part("name") name: RequestBody,
        @Part("documentType") type: RequestBody,
        @Part("notes") notes: RequestBody
    ): Response<JsonObject>

    @PATCH("api/documents/{id}")
    suspend fun update(
        @Header("Authorization") auth: String,
        @Path("id") id: String,
        @Body fields: JsonObject
    ): Response<JsonObject>

    @DELETE("api/documents/{id}")
    suspend fun delete(
        @Header("Authorization") auth: String,
        @Path("id") id: String
    ): Response<JsonObject>

    @GET("api/documents/{id}/download")
    suspend fun download(
        @Header("Authorization") auth: String,
        @Path("id") id: String
    ): Response<ResponseBody>
}
