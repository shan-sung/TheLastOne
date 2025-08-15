package com.example.thelastone.data.model

data class SavedPlace(
    val placeId: String,          // Google Maps Place ID
    val name: String,
    val lat: Double,
    val lng: Double,
    val rating: Double?,
    val userRatingsTotal: Int?,
    val address: String?,
    val openingHours: List<String>? = null, // 營業時間 (每天一條字串)
    val photoUrl: String? = null
)
