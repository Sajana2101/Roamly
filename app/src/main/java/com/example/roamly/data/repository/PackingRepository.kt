package com.example.roamly.data.repository

import android.content.Context
import com.example.roamly.data.model.PackingItem
import com.example.roamly.data.model.PackingList
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class PackingRepository(
    context: Context
) {

    private val sharedPreferences =
        context.getSharedPreferences(
            "roamly_packing",
            Context.MODE_PRIVATE
        )

    private val gson =
        Gson()

    fun getPackingLists():
            MutableList<PackingList> {

        val json =
            sharedPreferences.getString(
                PACKING_LISTS_KEY,
                null
            ) ?: return mutableListOf()

        return try {

            val type =
                object :
                    TypeToken<MutableList<PackingList>>() {}
                    .type

            gson.fromJson<MutableList<PackingList>>(
                json,
                type
            ) ?: mutableListOf()

        } catch (_: Exception) {

            mutableListOf()
        }
    }

    fun savePackingLists(
        packingLists:
        List<PackingList>
    ) {

        sharedPreferences
            .edit()
            .putString(
                PACKING_LISTS_KEY,
                gson.toJson(
                    packingLists
                )
            )
            .apply()
    }

    fun getPackingItems():
            MutableList<PackingItem> {

        val json =
            sharedPreferences.getString(
                PACKING_ITEMS_KEY,
                null
            ) ?: return mutableListOf()

        return try {

            val type =
                object :
                    TypeToken<MutableList<PackingItem>>() {}
                    .type

            gson.fromJson<MutableList<PackingItem>>(
                json,
                type
            ) ?: mutableListOf()

        } catch (_: Exception) {

            mutableListOf()
        }
    }

    fun savePackingItems(
        packingItems:
        List<PackingItem>
    ) {

        sharedPreferences
            .edit()
            .putString(
                PACKING_ITEMS_KEY,
                gson.toJson(
                    packingItems
                )
            )
            .apply()
    }

    companion object {

        private const val PACKING_LISTS_KEY =
            "packing_lists"

        private const val PACKING_ITEMS_KEY =
            "packing_items"
    }
}