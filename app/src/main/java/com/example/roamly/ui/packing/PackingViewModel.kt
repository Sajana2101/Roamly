package com.example.roamly.ui.packing

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.roamly.data.model.PackingItem
import com.example.roamly.data.model.PackingItemRequest
import com.example.roamly.data.model.PackingList
import com.example.roamly.data.model.PackingListRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PackingViewModel : ViewModel() {

    // LOCAL TESTING
    // This ViewModel currently uses local test data only.
    // The backend/API connection will be added later
    // When the API is connected, these local data sections can be
    // replaced with repository/API calls.



    // PACKING LISTS - Stores all packing lists currently displayed in the app.
    private val _packingLists = MutableStateFlow<List<PackingList>>(emptyList())

    val packingLists: StateFlow<List<PackingList>> = _packingLists



    // PACKING ITEMS
    private val _packingItems =  MutableStateFlow<List<PackingItem>>(emptyList())

    val packingItems: StateFlow<List<PackingItem>> =  _packingItems



    // PACKING PROGRESS
    // Stores the number of packed items and total items for each packing list.

    private val _packingProgress = MutableStateFlow<Map<String, Pair<Int, Int>>>(emptyMap())

    val packingProgress: StateFlow<Map<String, Pair<Int, Int>>> =  _packingProgress



    // LOCAL TEST ITEMS -  Temporary in-memory storage for packing items.
    // These items only exist while the app is running.
    // They are not saved to a database or backend.
    private val testPackingItems = mutableListOf<PackingItem>()



    // LOADING STATE  - Used by PackingFragment to control the ProgressBar.
    // Kept even though the current version uses local data,
    // so the progress bars load properly
    private val _isLoading = MutableStateFlow(false)

    val isLoading: StateFlow<Boolean> =_isLoading


    // ERROR STATE  - Stores an error message that can be displayed by the UI.
    // Kept so the Fragment can continue handling errors when API functionality is added later.
    private val _errorMessage = MutableStateFlow<String?>(null)

    val errorMessage: StateFlow<String?> = _errorMessage



    // GET ALL PACKING LISTS-  Currently there is no API call.
    // Packing lists are stored locally in _packingLists.
    // This method is kept so the Fragment can still call getPackingLists() when the screen opens.
    fun getPackingLists() {
        // No API call currently.
        // Local packing lists are already stored in _packingLists.
    }



    // CREATE A PACKING LIST - Creates a packing list locally for testing.
    fun createPackingList(
        request: PackingListRequest
    ) {

        val newList = PackingList(
            packingListId = "test-${System.currentTimeMillis()}",
            userId = "test-user",
            name = request.name,
            description = request.description,
            createdAt = "2026-09-16",
            updatedAt = "2026-09-16"
        )

        _packingLists.value =  _packingLists.value + newList
    }



    // UPDATE A PACKING LIST -  Updates a packing list in the local test data.
    fun updatePackingList(packingListId: String,request: PackingListRequest) {

        val index = _packingLists.value.indexOfFirst {
                it.packingListId == packingListId
            }

        if (index != -1) {
            val oldList =
                _packingLists.value[index]

            val updatedList = oldList.copy(
                    name = request.name,
                    description = request.description
                )

            val updatedLists = _packingLists.value.toMutableList()

            updatedLists[index] =updatedList

            _packingLists.value = updatedLists
        }
    }



    // DELETE A PACKING LIST - Deletes a packing list from the local test data.
    fun deletePackingList(packingListId: String) {

        _packingLists.value =
            _packingLists.value.filter {
                it.packingListId != packingListId
            }

        // Remove progress belonging to the deleted list.
        _packingProgress.value =
            _packingProgress.value
                .toMutableMap()
                .apply {
                    remove(packingListId)
                }

        // Remove any local items belonging to the deleted list.
        testPackingItems.removeAll {
            it.packingListId == packingListId
        }
    }



    // GET ITEMS FROM A PACKING LIST - Gets packing items from the local test list.
    fun getPackingItems(packingListId: String) {

        val items = testPackingItems.filter {
                it.packingListId == packingListId
            }

        _packingItems.value = items

        // Recalculate the progress for this packing list.
        updatePackingProgress(packingListId,items
        )
    }



    // ADD AN ITEM TO A PACKING LIST -  Adds an item to the local test data.
    fun addPackingItem(packingListId: String,request: PackingItemRequest) {

        val newItem = PackingItem(
            packingItemId ="test-item-${System.currentTimeMillis()}",
            packingListId = packingListId,
            name = request.name,
            isPacked = request.isPacked,
            createdAt = "2026-09-17",
            updatedAt = "2026-09-17"
        )

        testPackingItems.add(newItem)

        // Refresh the displayed items and progress.
        getPackingItems(packingListId)
    }


    // UPDATE AN ITEM - Updates an item in the local test data.
    fun updatePackingItem(itemId: String,request: PackingItemRequest) {

        val index =
            testPackingItems.indexOfFirst {
                it.packingItemId == itemId
            }

        if (index != -1) {
            val oldItem = testPackingItems[index]

            testPackingItems[index] = oldItem.copy(
                    name = request.name,
                    isPacked = request.isPacked
                )

            // Refresh the displayed items and progress.
            getPackingItems(
                oldItem.packingListId
            )
        }
    }


    // CALCULATE PACKING PROGRESS
    // Counts how many items are packed and how many items exist in total for a particular packing list.
    private fun updatePackingProgress(packingListId: String, items: List<PackingItem>) {

        val packedItems = items.count {
                it.isPacked
            }

        val totalItems = items.size

        _packingProgress.value = _packingProgress.value
                .toMutableMap()
                .apply {

                    this[packingListId] =
                        Pair(
                            packedItems,
                            totalItems
                        )
                }
    }



    // DELETE AN ITEM - Deletes an item from the local test data.
    fun deletePackingItem(itemId: String,packingListId: String) {

        testPackingItems.removeAll {
            it.packingItemId == itemId
        }

        // Refresh the displayed items and progress.
        getPackingItems(packingListId)
    }



    // CLEAR ERROR MESSAGE -  Clears the current error after it has been displayed.
    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}