package com.example.roamly.data.model

data class TravelDocument(
    val documentId: String,
    val displayName: String,
    val mimeType: String,
    val uri: String,
    val sizeBytes: Long = 0L,
    val addedAt: Long
)