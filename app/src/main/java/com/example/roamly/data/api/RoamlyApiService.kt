package com.example.roamly.data.api

import com.example.roamly.data.model.PackingItem
import com.example.roamly.data.model.PackingItemRequest
import com.example.roamly.data.model.PackingList
import com.example.roamly.data.model.PackingListRequest
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface RoamlyApiService {

    // Get all packing lists
    @GET("api/packing-lists")
    suspend fun getPackingLists(): List<PackingList>

    // Create a packing list
    @POST("api/packing-lists")
    suspend fun createPackingList(@Body request: PackingListRequest): PackingList

    // update a packing list
    @PUT("api/packing-lists/{packingListId}")
    suspend fun updatePackingList(@Path("packingListId") packingListId: String, @Body request: PackingListRequest): PackingList

    // delete a packing list
    @DELETE("api/packing-lists/{packingListId}")
    suspend fun deletePackingList(@Path("packingListId") packingListId: String)

    // Get all items in a packing list
    @GET("api/packing-lists/{packingListId}/items")
    suspend fun getPackingListItems(@Path("packingListId") packingListId: String): List<PackingItem>

    // add an item
    @POST("api/packing-lists/{packingListId}/items")
    suspend fun addPackingItem(@Path("itemId") itemId: String, @Body request: PackingItemRequest): PackingItem

    // update an item
    @PUT("api/packing-lists-items/{itemId}")
    suspend fun updatePackingItem(@Path("itemId") itemId: String, @Body request: PackingItemRequest): PackingItem

    // Delete an item
    @DELETE("api/packing--items/{itemId}")
    suspend fun deletePackingItem(@Path("itemId") itemId: String)
}


