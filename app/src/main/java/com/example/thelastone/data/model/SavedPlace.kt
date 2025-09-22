package com.example.thelastone.data.model

data class SavedPlace(
    val id: String,
    val userId: String,
    val place: PlaceLite,
    val savedAt: Long
)