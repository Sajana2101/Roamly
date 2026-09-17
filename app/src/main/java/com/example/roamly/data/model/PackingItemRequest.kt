package com.example.roamly.data.model

data class PackingItemRequest(

    // used to add/edit an item
    // also allows user to tick/untick an item

    val name: String,
    val isPacked: Boolean
)
