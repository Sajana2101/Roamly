package com.example.roamly.ui.packing

import android.util.Log
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

class PackingViewModel : ViewModel() {

    // DELETE THESE 2 LINES BEFORE SUBMISSION!!!
    // Temporary test mode while the backend is being developed
    private val useTestData = true

    // Repository used to communicate with the backend API
    private val repository = PackingRepository(RetrofitClient.apiService)

    // Stores all packing lists
    private val _packingLists = MutableStateFlow<List<PackingList>>(emptyList())

    val packingLists: StateFlow<List<PackingList>> = _packingLists

    // Stores the items for the currently selected packing list
    private val _packingItems = MutableStateFlow<List<PackingItem>>(emptyList())

    val packingItems: StateFlow<List<PackingItem>> =  _packingItems

    // Stores packing progress for each packing list
    // Pair = packed items, total items
    private val _packingProgress = MutableStateFlow<Map<String, Pair<Int, Int>>>(emptyMap())

    val packingProgress: StateFlow<Map<String, Pair<Int, Int>>> = _packingProgress

    // Local test items used while the backend is unavailable
    private val testPackingItems = mutableListOf<PackingItem>()

    // Stores whether data is currently loading
    private val _isLoading =  MutableStateFlow(false)

    val isLoading: StateFlow<Boolean> = _isLoading

    // Stores error messages
    private val _errorMessage = MutableStateFlow<String?>(null)

    val errorMessage: StateFlow<String?> =_errorMessage


    // GET ALL PACKING LISTS
    fun getPackingLists() {

        // DELETE THESE LINES BEFORE SUBMISSION!!
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

                _errorMessage.value =
                    e.message ?: "Could not get packing lists"

            }

            _isLoading.value = false
        }
    }


    // CREATE A PACKING LIST
    fun createPackingList(request: PackingListRequest) {

        // Use local test data while the backend is unavailable
        if (useTestData) {

            val newList = PackingList(
                packingListId =
                    "test-${System.currentTimeMillis()}",
                userId = "test-user",
                name = request.name,
                description = request.description,
                createdAt = "2026-09-16",
                updatedAt = "2026-09-16"
            )

            _packingLists.value =
                _packingLists.value + newList

            return
        }

        viewModelScope.launch {
            try {

                repository.createPackingList(request)

                // Refresh the lists after creating a new one
                getPackingLists()

            } catch (e: Exception) {

                _errorMessage.value =
                    e.message ?: "Could not create packing list"
            }
        }
    }


    // UPDATE A PACKING LIST
    fun updatePackingList(
        packingListId: String,
        request: PackingListRequest
    ) {

        // Use local test data while the backend is unavailable
        if (useTestData) {

            val index =
                _packingLists.value.indexOfFirst {
                    it.packingListId == packingListId
                }

            if (index != -1) {

                val oldList =
                    _packingLists.value[index]

                val updatedList =
                    oldList.copy(
                        name = request.name,
                        description = request.description
                    )

                val updatedLists =
                    _packingLists.value.toMutableList()

                updatedLists[index] = updatedList

                _packingLists.value =
                    updatedLists
            }

            return
        }

        viewModelScope.launch {
            try {

                repository.updatePackingList(
                    packingListId,
                    request
                )

                // Refresh the lists after updating one
                getPackingLists()

            } catch (e: Exception) {

                _errorMessage.value =
                    e.message ?: "Could not update packing list"
            }
        }
    }


    // DELETE A PACKING LIST
    fun deletePackingList(packingListId: String) {

        // Use local test data while the backend is unavailable
        if (useTestData) {

            _packingLists.value =
                _packingLists.value.filter {
                    it.packingListId != packingListId
                }

            // Remove the progress for the deleted list
            _packingProgress.value =
                _packingProgress.value
                    .toMutableMap()
                    .apply {
                        remove(packingListId)
                    }

            return
        }

        viewModelScope.launch {
            try {

                repository.deletePackingList(
                    packingListId
                )

                // Refresh the lists after deleting one
                getPackingLists()

            } catch (e: Exception) {

                _errorMessage.value =
                    e.message ?: "Could not delete packing list"
            }
        }
    }


    // GET ITEMS FROM A PACKING LIST
    fun getPackingItems(packingListId: String) {

        // DELETE THESE LINES BEFORE SUBMISSION!!
        // Use local test data while the backend is unavailable
        if (useTestData) {

            val items =
                testPackingItems.filter {
                    it.packingListId == packingListId
                }

            _packingItems.value = items

            // Update the progress for this packing list
            updatePackingProgress(
                packingListId,
                items
            )

            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {

                val items =
                    repository.getPackingListItems(
                        packingListId
                    )

                _packingItems.value = items

                // Update the progress for this packing list
                updatePackingProgress(
                    packingListId,
                    items
                )

            } catch (e: Exception) {

                _errorMessage.value =
                    e.message ?: "Could not get packing items"

            }

            _isLoading.value = false
        }
    }


    // ADD AN ITEM TO A PACKING LIST
    fun addPackingItem(
        packingListId: String,
        request: PackingItemRequest
    ) {

        // DELETE THESE LINES BEFORE SUBMISSION!!
        // Use local test data while the backend is unavailable
        if (useTestData) {

            val newItem = PackingItem(
                packingItemId =
                    "test-item-${System.currentTimeMillis()}",
                packingListId = packingListId,
                name = request.name,
                isPacked = request.isPacked,
                createdAt = "2026-09-17",
                updatedAt = "2026-09-17"
            )

            testPackingItems.add(newItem)

            // Refresh items and progress
            getPackingItems(packingListId)

            return
        }

        viewModelScope.launch {
            try {

                repository.addPackingItem(
                    packingListId,
                    request
                )

                // Refresh the list after adding an item
                getPackingItems(packingListId)

            } catch (e: Exception) {

                _errorMessage.value =
                    e.message ?: "Could not add packing item"
            }
        }
    }


    // UPDATE AN ITEM IN A PACKING LIST
    fun updatePackingItem(
        itemId: String,
        request: PackingItemRequest
    ) {

        // Use local test data while the backend is unavailable
        if (useTestData) {

            val index =
                testPackingItems.indexOfFirst {
                    it.packingItemId == itemId
                }

            if (index != -1) {

                val oldItem =
                    testPackingItems[index]

                testPackingItems[index] =
                    oldItem.copy(
                        name = request.name,
                        isPacked = request.isPacked
                    )

                // Refresh items and progress
                getPackingItems(
                    oldItem.packingListId
                )
            }

            return
        }

        viewModelScope.launch {
            try {

                repository.updatePackingItem(
                    itemId,
                    request
                )

            } catch (e: Exception) {

                _errorMessage.value =
                    e.message ?: "Could not update packing item"
            }
        }
    }


    // CALCULATE PACKING PROGRESS
    // Stores packed items and total items for each list
    private fun updatePackingProgress(
        packingListId: String,
        items: List<PackingItem>
    ) {

        val packedItems =
            items.count { it.isPacked }

        val totalItems =
            items.size

        _packingProgress.value =
            _packingProgress.value
                .toMutableMap()
                .apply {

                    this[packingListId] =
                        Pair(
                            packedItems,
                            totalItems
                        )
                }
    }


    // DELETE AN ITEM FROM A PACKING LIST
    fun deletePackingItem(
        itemId: String,
        packingListId: String
    ) {

        // Use local test data while the backend is unavailable
        if (useTestData) {

            testPackingItems.removeAll {
                it.packingItemId == itemId
            }

            // Refresh items and progress
            getPackingItems(packingListId)

            Log.d(
                "PackingViewModel",
                "Items after delete: ${_packingItems.value}"
            )

            return
        }

        viewModelScope.launch {
            try {

                repository.deletePackingItem(itemId)

                // Refresh the list after deleting an item
                getPackingItems(packingListId)

            } catch (e: Exception) {

                _errorMessage.value =
                    e.message ?: "Could not delete packing item"
            }
        }
    }


    // CLEAR ERROR MESSAGE
    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}