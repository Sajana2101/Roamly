package com.example.roamly.data.auth

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

data class RoamlyGoogleAuthRequest(
    val idToken: String
)

data class RoamlyAuthUser(
    val userId: String,
    val name: String,
    val email: String,
    val ssoProvider: String?,
    val providerUserId: String?,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

data class RoamlyAuthResponse(
    val accessToken: String,
    val user: RoamlyAuthUser
)

interface RoamlyAuthApi {

    @POST("api/auth/google")
    suspend fun authenticateWithGoogle(
        @Body request: RoamlyGoogleAuthRequest
    ): retrofit2.Response<RoamlyAuthResponse>
}

object RoamlyAuthClient {

    private const val BASE_URL =
        "https://roamly-api-sujen-g7d4bscfbba6aaex.southafricanorth-01.azurewebsites.net/"

    private val httpClient =
        OkHttpClient.Builder()
            .connectTimeout(
                15,
                TimeUnit.SECONDS
            )
            .readTimeout(
                30,
                TimeUnit.SECONDS
            )
            .writeTimeout(
                30,
                TimeUnit.SECONDS
            )
            .callTimeout(
                45,
                TimeUnit.SECONDS
            )
            .build()

    private val retrofit =
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(httpClient)
            .addConverterFactory(
                GsonConverterFactory.create()
            )
            .build()

    val api: RoamlyAuthApi =
        retrofit.create(
            RoamlyAuthApi::class.java
        )
}

class RoamlyAuthRepository {

    suspend fun authenticateWithGoogle(
        googleIdToken: String
    ): RoamlyAuthResponse {

        val response =
            RoamlyAuthClient.api
                .authenticateWithGoogle(
                    RoamlyGoogleAuthRequest(
                        idToken =
                            googleIdToken
                    )
                )

        if (!response.isSuccessful) {

            val errorText =
                response.errorBody()
                    ?.string()
                    .orEmpty()

            throw IllegalStateException(
                "Roamly API authentication failed. " +
                        "HTTP ${response.code()} " +
                        errorText
            )
        }

        return response.body()
            ?: throw IllegalStateException(
                "Roamly API returned an empty response."
            )
    }
}