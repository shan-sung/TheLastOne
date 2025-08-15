package com.example.thelastone.data.model

data class TripForm(
    val name: String,
    val budget: Int?,
    val dateRange: DateRange,
    val transportPreferences: List<String>,
    val useGmapsRating: Boolean,
    val styles: List<String>
)
