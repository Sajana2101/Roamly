package com.example.roamly.data.model

data class PackingItem(

    // represents 1 item inside a packing list

    val packingItemId: String,
    val packingListId: String,
    val name: String,
    val isPacked: Boolean,
    val createdAt: String,
    val updatedAt: String
)
