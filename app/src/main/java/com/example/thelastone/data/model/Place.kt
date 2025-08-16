package com.example.thelastone.data.model

data class Place(
    val placeId: String,
    val name: String,
    val rating: Double?,                   // << 改成 Double? 解決你的型別錯誤
    val userRatingsTotal: Int?,
    val address: String?,
    val openingHours: List<String> = emptyList(),
    val lat: Double,
    val lng: Double,
    val photoUrl: String? = null,
    val miniMapUrl: String? = null
)