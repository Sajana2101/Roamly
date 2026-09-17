package com.example.roamly.data.model

data class PackingList(

    val packingListId: String,
    val userId: String,
    val name: String,
    val description: String?,
    val createdAt: String,
    val updatedAt: String
)
