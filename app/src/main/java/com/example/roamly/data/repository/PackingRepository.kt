package com.example.roamly.data.repository

import com.example.roamly.data.api.RoamlyApiService
import com.example.roamly.data.model.PackingItem
import com.example.roamly.data.model.PackingItemRequest
import com.example.roamly.data.model.PackingList
import com.example.roamly.data.model.PackingListRequest

class PackingRepository(
    // connect to api service
    private val apiService: RoamlyApiService
){

    // Get all packing lists
    suspend fun getPackingLists(): List<PackingList> {
        return apiService.getPackingLists()
    }

    // create a packing list
    suspend fun createPackingList(request: PackingListRequest): PackingList {
        return apiService.createPackingList(request)
    }

    // update a packing list
    suspend fun updatePackingList(packingListId: String, request: PackingListRequest): PackingList {
        return apiService.updatePackingList(packingListId, request)
    }

    // delete a packing list
    suspend fun deletePackingList(packingListId: String) {
        apiService.deletePackingList(packingListId)
    }

    // get items in a packing list
    suspend fun getPackingListItems(packingListId: String): List<PackingItem> {
        return apiService.getPackingListItems(packingListId)
    }

    // add an item
    suspend fun addPackingItem(packingListId: String, request: PackingItemRequest): PackingItem {
        return apiService.addPackingItem(packingListId, request)
    }

    // update an item
    suspend fun updatePackingItem(itemId: String, request: PackingItemRequest): PackingItem {
        return apiService.updatePackingItem(itemId, request)
    }

    // delete an item
    suspend fun deletePackingItem(itemId: String) {
        apiService.deletePackingItem(itemId)
    }


}


