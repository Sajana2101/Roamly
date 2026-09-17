package com.example.roamly.ui.packing

import androidx.annotation.Nullable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.roamly.data.api.RetrofitClient
import com.example.roamly.data.model.PackingItem
import com.example.roamly.data.model.PackingItemRequest
import com.example.roamly.data.model.PackingList
import com.example.roamly.data.model.PackingListRequest
import com.example.roamly.data.repository.PackingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PackingViewModel: ViewModel() {

    // DELETE THESE 2 LINES BEFORE SUBMISSION!!!
    // Temporary test mode while the backend is being developed
    private val useTestData = true

    private val repository = PackingRepository(RetrofitClient.apiService)

    private val _packingLists = MutableStateFlow<List<PackingList>>(emptyList())
    val packingLists: StateFlow<List<PackingList>> = _packingLists

    private val _packingItems = MutableStateFlow<List<PackingItem>>(emptyList())
    val packingItems: StateFlow<List<PackingItem>> = _packingItems

    //to ensure items are displayed properly for every packing list
    private val testPackingItems = mutableListOf<PackingItem>()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    // get all packing lists
    fun getPackingLists() {

        // DELETE THESE 2 LINES BEFORE SUBMISSION!!
        // Use local test data while the backend is unavailable
        if (useTestData) {
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val lists = repository.getPackingLists()
                  _packingLists.value = lists
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Could not get packing lists"

            }
            _isLoading.value = false
        }
    }

    // create a packing list
    fun createPackingList(request: PackingListRequest) {

        // Use local test data while the backend is unavailable
        if (useTestData) {
            val newList = PackingList(
                packingListId = "test-${System.currentTimeMillis()}",
                userId = "test-user",
                name = request.name,
                description = request.description,
                createdAt = "2026-09-16",
                updatedAt = "2026-09-16"
            )
            _packingLists.value = _packingLists.value + newList
            return
        }


        viewModelScope.launch {
            try {
                repository.createPackingList(request)
                //refresh the lists are creating a new one
                getPackingLists()
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Could not create packing list"
            }
        }

    }


    // update a packing list
    fun updatePackingList(packingListId: String, request: PackingListRequest) {

        viewModelScope.launch {
            try {
                repository.updatePackingList(packingListId, request)
                //refresh the lists are updating one
                getPackingLists()
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Could not update packing list"
            }
        }

    }

    // delete a packing list
    fun deletePackingList(packingListId: String) {
        viewModelScope.launch {
            try {
                repository.deletePackingList(packingListId)
                //refresh the lists are deleting one
                getPackingLists()
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Could not delete packing list"
            }
        }
    }

    // get items from the list
    fun getPackingItems(packingListId: String) {

        // DELETE THE IF-STATEMENT BELOW THESE LINES BEFORE SUBMISSION!!
        // Use local test data while the backend is unavailable
        if (useTestData) {
            _packingItems.value =
                testPackingItems.filter {
                    it.packingListId == packingListId
                }
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val items = repository.getPackingListItems(packingListId)
                _packingItems.value = items
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Could not get packing items"
            }
            _isLoading.value = false
        }
    }

    // add item to a list
    fun addPackingItem(packingListId: String, request: PackingItemRequest) {

        // DELETE THE IS-STATEMENT LINES BEFORE SUBMISSION!!
        // Use local test data while the backend is unavailable
        if (useTestData) {
            val newItem = PackingItem(
                packingItemId = "test-item-${System.currentTimeMillis()}",
                packingListId = packingListId,
                name = request.name,
                isPacked = request.isPacked,
                createdAt = "2026-09-17",
                updatedAt = "2026-09-17"
            )
            testPackingItems.add(newItem)
            getPackingItems(packingListId)
            return
        }


        viewModelScope.launch {
            try {
                repository.addPackingItem(packingListId, request)
                //refresh the list after adding an item
                getPackingItems(packingListId)
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Could not add packing item"
            }
        }
    }

    // update an item in a list
    fun updatePackingItem(itemId: String, request: PackingItemRequest) {

        if (useTestData) {
            val index = testPackingItems.indexOfFirst {
                it.packingItemId == itemId
            }

            if (index != -1) {
                val oldItem = testPackingItems[index]
                testPackingItems[index] = oldItem.copy(
                    name = request.name,
                    isPacked = request.isPacked
                )
                getPackingItems(oldItem.packingListId)
            }
            return
        }



        viewModelScope.launch {
            try {
                repository.updatePackingItem(itemId, request)
                //refresh the list after updating an item
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Could not update packing item"
            }
        }
    }


    // delete an item from a list
    fun deletePackingItem(itemId: String, packingListId: String) {
        viewModelScope.launch {
            try {
                repository.deletePackingItem(itemId)
                //refresh the list after deleting an item
                getPackingItems(packingListId)
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Could not delete packing item"
            }
        }
    }

    // clear error message
    fun clearErrorMessage() {
        _errorMessage.value = null
    }

}