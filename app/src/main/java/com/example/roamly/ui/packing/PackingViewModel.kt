package com.example.roamly.ui.packing

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import com.example.roamly.data.model.PackingItem
import com.example.roamly.data.model.PackingItemRequest
import com.example.roamly.data.model.PackingList
import com.example.roamly.data.model.PackingListRequest
import com.example.roamly.data.repository.PackingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class PackingViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository =
        PackingRepository(
            application.applicationContext
        )

    private val _packingLists =
        MutableStateFlow<List<PackingList>>(
            emptyList()
        )

    val packingLists:
            StateFlow<List<PackingList>> =
        _packingLists

    private val _packingItems =
        MutableStateFlow<List<PackingItem>>(
            emptyList()
        )

    val packingItems:
            StateFlow<List<PackingItem>> =
        _packingItems

    private val _packingProgress =
        MutableStateFlow<
                Map<String, Pair<Int, Int>>
                >(
            emptyMap()
        )

    val packingProgress:
            StateFlow<
                    Map<String, Pair<Int, Int>>
                    > =
        _packingProgress

    private val _isLoading =
        MutableStateFlow(
            false
        )

    val isLoading:
            StateFlow<Boolean> =
        _isLoading

    private val _errorMessage =
        MutableStateFlow<String?>(
            null
        )

    val errorMessage:
            StateFlow<String?> =
        _errorMessage

    init {

        loadPackingData()
    }

    fun getPackingLists() {

        loadPackingData()
    }

    fun createPackingList(
        request:
        PackingListRequest
    ) {

        try {

            val lists =
                repository
                    .getPackingLists()

            val timestamp =
                currentTimestamp()

            val newList =
                PackingList(
                    packingListId =
                        UUID.randomUUID()
                            .toString(),
                    userId =
                        LOCAL_USER_ID,
                    name =
                        request.name,
                    description =
                        request.description,
                    createdAt =
                        timestamp,
                    updatedAt =
                        timestamp
                )

            lists.add(
                newList
            )

            repository
                .savePackingLists(
                    lists
                )

            Log.i(
                "RoamlyPacking",
                "Packing list created: ${newList.packingListId}"
            )

            loadPackingData()

        } catch (
            exception: Exception
        ) {

            handleError(
                "Could not create packing list.",
                exception
            )
        }
    }

    fun updatePackingList(
        packingListId: String,
        request:
        PackingListRequest
    ) {

        try {

            val lists =
                repository
                    .getPackingLists()

            val index =
                lists.indexOfFirst {
                    it.packingListId ==
                            packingListId
                }

            if (index == -1) {

                _errorMessage.value =
                    "Packing list could not be found."

                return
            }

            val currentList =
                lists[index]

            val updatedList =
                currentList.copy(
                    name =
                        request.name,
                    description =
                        request.description,
                    updatedAt =
                        currentTimestamp()
                )

            lists[index] =
                updatedList

            repository
                .savePackingLists(
                    lists
                )

            Log.i(
                "RoamlyPacking",
                "Packing list updated: $packingListId"
            )

            loadPackingData()

        } catch (
            exception: Exception
        ) {

            handleError(
                "Could not update packing list.",
                exception
            )
        }
    }

    fun deletePackingList(
        packingListId: String
    ) {

        try {

            val lists =
                repository
                    .getPackingLists()

            val items =
                repository
                    .getPackingItems()

            lists.removeAll {
                it.packingListId ==
                        packingListId
            }

            items.removeAll {
                it.packingListId ==
                        packingListId
            }

            repository
                .savePackingLists(
                    lists
                )

            repository
                .savePackingItems(
                    items
                )

            _packingItems.value =
                _packingItems.value
                    .filter {
                        it.packingListId !=
                                packingListId
                    }

            Log.i(
                "RoamlyPacking",
                "Packing list deleted: $packingListId"
            )

            loadPackingData()

        } catch (
            exception: Exception
        ) {

            handleError(
                "Could not delete packing list.",
                exception
            )
        }
    }

    fun getPackingItems(
        packingListId: String
    ) {

        try {

            val allItems =
                repository
                    .getPackingItems()

            val selectedItems =
                allItems
                    .filter {
                        it.packingListId ==
                                packingListId
                    }

            _packingItems.value =
                selectedItems

            updateAllPackingProgress()

        } catch (
            exception: Exception
        ) {

            handleError(
                "Could not load packing items.",
                exception
            )
        }
    }

    fun addPackingItem(
        packingListId: String,
        request:
        PackingItemRequest
    ) {

        try {

            val items =
                repository
                    .getPackingItems()

            val timestamp =
                currentTimestamp()

            val newItem =
                PackingItem(
                    packingItemId =
                        UUID.randomUUID()
                            .toString(),
                    packingListId =
                        packingListId,
                    name =
                        request.name,
                    isPacked =
                        request.isPacked,
                    createdAt =
                        timestamp,
                    updatedAt =
                        timestamp
                )

            items.add(
                newItem
            )

            repository
                .savePackingItems(
                    items
                )

            Log.i(
                "RoamlyPacking",
                "Packing item created: ${newItem.packingItemId}"
            )

            getPackingItems(
                packingListId
            )

        } catch (
            exception: Exception
        ) {

            handleError(
                "Could not add packing item.",
                exception
            )
        }
    }

    fun updatePackingItem(
        itemId: String,
        request:
        PackingItemRequest
    ) {

        try {

            val items =
                repository
                    .getPackingItems()

            val index =
                items.indexOfFirst {
                    it.packingItemId ==
                            itemId
                }

            if (index == -1) {

                _errorMessage.value =
                    "Packing item could not be found."

                return
            }

            val currentItem =
                items[index]

            val updatedItem =
                currentItem.copy(
                    name =
                        request.name,
                    isPacked =
                        request.isPacked,
                    updatedAt =
                        currentTimestamp()
                )

            items[index] =
                updatedItem

            repository
                .savePackingItems(
                    items
                )

            Log.i(
                "RoamlyPacking",
                "Packing item updated: $itemId"
            )

            getPackingItems(
                currentItem
                    .packingListId
            )

        } catch (
            exception: Exception
        ) {

            handleError(
                "Could not update packing item.",
                exception
            )
        }
    }

    fun deletePackingItem(
        itemId: String,
        packingListId: String
    ) {

        try {

            val items =
                repository
                    .getPackingItems()

            items.removeAll {
                it.packingItemId ==
                        itemId
            }

            repository
                .savePackingItems(
                    items
                )

            Log.i(
                "RoamlyPacking",
                "Packing item deleted: $itemId"
            )

            getPackingItems(
                packingListId
            )

        } catch (
            exception: Exception
        ) {

            handleError(
                "Could not delete packing item.",
                exception
            )
        }
    }

    private fun loadPackingData() {

        _isLoading.value =
            true

        try {

            val lists =
                repository
                    .getPackingLists()

            _packingLists.value =
                lists

            updateAllPackingProgress()

            Log.d(
                "RoamlyPacking",
                "Loaded ${lists.size} packing lists"
            )

        } catch (
            exception: Exception
        ) {

            handleError(
                "Could not load packing lists.",
                exception
            )

        } finally {

            _isLoading.value =
                false
        }
    }

    private fun updateAllPackingProgress() {

        val lists =
            repository
                .getPackingLists()

        val items =
            repository
                .getPackingItems()

        val progress =
            mutableMapOf<
                    String,
                    Pair<Int, Int>
                    >()

        lists.forEach { list ->

            val listItems =
                items.filter {
                    it.packingListId ==
                            list.packingListId
                }

            val packedItems =
                listItems.count {
                    it.isPacked
                }

            progress[
                list.packingListId
            ] =
                Pair(
                    packedItems,
                    listItems.size
                )
        }

        _packingProgress.value =
            progress
    }

    fun clearErrorMessage() {

        _errorMessage.value =
            null
    }

    private fun currentTimestamp():
            String {

        return SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss",
            Locale.getDefault()
        ).format(
            Date()
        )
    }

    private fun handleError(
        message: String,
        exception: Exception
    ) {

        Log.e(
            "RoamlyPacking",
            message,
            exception
        )

        _errorMessage.value =
            message
    }

    companion object {

        private const val LOCAL_USER_ID =
            "local-user"
    }
}