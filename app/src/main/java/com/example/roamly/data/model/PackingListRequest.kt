package com.example.roamly.data.model

data class PackingListRequest(
    // when user clicks on '+ Create List'
    // making a new packing list

    val name: String,
    val description: String?
)
