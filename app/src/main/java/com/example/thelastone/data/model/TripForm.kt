package com.example.thelastone.data.model

data class TripForm(
    val name: String,
    val totalBudget: Int?,                 // 單位：元
    val startDate: String,                 // yyyy-MM-dd
    val endDate: String,                   // yyyy-MM-dd
    val transportPreferences: List<String>,
    val useGmapsRating: Boolean,
    val styles: List<String>
)