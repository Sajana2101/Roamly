package com.example.roamly.data.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    private const val BASE_URL = "https://roamly-api-sujen-g7d4bscfbba6aaex.southafricanorth-01.azurewebsites.net/"

    val apiService: RoamlyApiService by lazy {

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(RoamlyApiService::class.java)
    }

}