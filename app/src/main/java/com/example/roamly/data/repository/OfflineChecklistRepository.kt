
package com.example.roamly.data.repository

import android.content.Context
import com.example.roamly.data.auth.SessionManager
import com.example.roamly.data.local.RoomTravelStore
import java.util.UUID

data class OfflineChecklist(
    val checklistId: String,
    val holidayId: Int,
    val title: String,
    val updatedAtMillis: Long =
        System.currentTimeMillis()
)

data class OfflineChecklistItem(
    val itemId: String,
    val checklistId: String,
    val title: String,
    val isCompleted: Boolean = false,
    val updatedAtMillis: Long =
        System.currentTimeMillis()
)

class OfflineChecklistRepository(
    context: Context
) {
    private val userId =
        SessionManager(context).getUserId()
            ?: throw IllegalStateException(
                "Sign in to access checklists."
            )

    private val store = RoomTravelStore(context)

    private val preferenceName =
        "roamly_checklists_$userId"

    suspend fun getChecklists(): List<OfflineChecklist> {
        return store.read(
            "checklist",
            preferenceName,
            "checklists",
            OfflineChecklist::class.java,
            { it.checklistId }
        )
    }

    suspend fun createChecklist(
        holidayId: Int,
        title: String
    ): OfflineChecklist {
        require(title.isNotBlank())

        val checklist = OfflineChecklist(
            checklistId = UUID.randomUUID().toString(),
            holidayId = holidayId,
            title = title.trim()
        )

        val checklists = getChecklists().toMutableList()
        checklists.add(checklist)

        store.replace(
            "checklist",
            preferenceName,
            "checklists",
            OfflineChecklist::class.java,
            checklists,
            { it.checklistId }
        )

        return checklist
    }

    suspend fun deleteChecklist(
        checklistId: String
    ) {
        val checklists = getChecklists()
            .filterNot {
                it.checklistId == checklistId
            }

        store.replace(
            "checklist",
            preferenceName,
            "checklists",
            OfflineChecklist::class.java,
            checklists,
            { it.checklistId }
        )

        val items = getAllItems()
            .filterNot {
                it.checklistId == checklistId
            }

        saveAllItems(items)
    }

    suspend fun getItems(
        checklistId: String
    ): List<OfflineChecklistItem> {
        return getAllItems()
            .filter {
                it.checklistId == checklistId
            }
    }

    suspend fun addItem(
        checklistId: String,
        title: String
    ): OfflineChecklistItem {
        require(title.isNotBlank())

        require(
            getChecklists().any {
                it.checklistId == checklistId
            }
        ) {
            "Checklist does not exist."
        }

        val item = OfflineChecklistItem(
            itemId = UUID.randomUUID().toString(),
            checklistId = checklistId,
            title = title.trim()
        )

        val items = getAllItems().toMutableList()
        items.add(item)

        saveAllItems(items)

        return item
    }

    suspend fun setItemCompleted(
        itemId: String,
        completed: Boolean
    ) {
        val items = getAllItems().map {
            if (it.itemId == itemId) {
                it.copy(
                    isCompleted = completed,
                    updatedAtMillis =
                        System.currentTimeMillis()
                )
            } else {
                it
            }
        }

        saveAllItems(items)
    }

    suspend fun deleteItem(
        itemId: String
    ) {
        val items = getAllItems()
            .filterNot {
                it.itemId == itemId
            }

        saveAllItems(items)
    }

    private suspend fun getAllItems():
            List<OfflineChecklistItem> {
        return store.read(
            "checklist_item",
            preferenceName,
            "checklist_items",
            OfflineChecklistItem::class.java,
            { it.itemId }
        )
    }

    private suspend fun saveAllItems(
        items: List<OfflineChecklistItem>
    ) {
        store.replace(
            "checklist_item",
            preferenceName,
            "checklist_items",
            OfflineChecklistItem::class.java,
            items,
            { it.itemId }
        )
    }
}
