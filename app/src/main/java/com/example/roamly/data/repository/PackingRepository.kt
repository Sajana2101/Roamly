
package com.example.roamly.data.repository

import android.content.Context
import com.example.roamly.data.auth.SessionManager
import com.example.roamly.data.local.RoomTravelStore
import com.example.roamly.data.model.PackingItem
import com.example.roamly.data.model.PackingList
import com.example.roamly.data.sync.PackingRestoreWorker
import com.example.roamly.data.sync.RoamlySyncScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class PackingRepository(context: Context) {

    private val appContext = context.applicationContext

    private val userId =
        SessionManager(appContext).getUserId()
            ?: throw IllegalStateException(
                "Sign in to access packing lists."
            )

    private val store = RoomTravelStore(appContext)

    private val preferenceName = "roamly_packing_$userId"

    init {
        PackingRestoreWorker.request(appContext)
    }

    fun getPackingLists(): MutableList<PackingList> =
        runBlocking(Dispatchers.IO) {
            store.read(
                "packing_list",
                preferenceName,
                "packing_lists",
                PackingList::class.java,
                { it.packingListId }
            )
        }

    fun savePackingLists(packingLists: List<PackingList>) {
        runBlocking(Dispatchers.IO) {
            store.replace(
                "packing_list",
                preferenceName,
                "packing_lists",
                PackingList::class.java,
                packingLists,
                { it.packingListId }
            )
        }

        RoamlySyncScheduler.requestNow(appContext)
    }

    fun getPackingItems(): MutableList<PackingItem> =
        runBlocking(Dispatchers.IO) {
            store.read(
                "packing_item",
                preferenceName,
                "packing_items",
                PackingItem::class.java,
                { it.packingItemId }
            )
        }

    fun savePackingItems(packingItems: List<PackingItem>) {
        runBlocking(Dispatchers.IO) {
            store.replace(
                "packing_item",
                preferenceName,
                "packing_items",
                PackingItem::class.java,
                packingItems,
                { it.packingItemId }
            )
        }

        RoamlySyncScheduler.requestNow(appContext)
    }
}
