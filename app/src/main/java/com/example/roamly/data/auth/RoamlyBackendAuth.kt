package com.example.roamly.data.auth

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

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
        "http://10.0.2.2:3000/"

    private val retrofit: Retrofit =
        Retrofit.Builder()
            .baseUrl(BASE_URL)
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
                        idToken = googleIdToken
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