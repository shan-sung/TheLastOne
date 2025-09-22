package com.example.thelastone.data.model

import kotlinx.serialization.Serializable

@Serializable
data class PlaceDetails(
    val placeId: String,
    val name: String,
    val address: String?,
    val lat: Double,
    val lng: Double,
    val rating: Double?,
    val userRatingsTotal: Int?,
    val photoUrl: String?,
    val types: List<String> = emptyList(),
    val websiteUri: String? = null,
    val nationalPhoneNumber: String? = null,
    val priceLevel: Int? = null,
    val openingHours: List<String> = emptyList(), // weekdayDescriptions
    val openNow: Boolean? = null,
    val openStatusText: String? = null
)

@Serializable
data class PlaceLite(
    val placeId: String,
    val name: String,
    val lat: Double,
    val lng: Double,
    val address: String? = null,
    val rating: Double? = null,
    val userRatingsTotal: Int? = null,
    val photoUrl: String? = null,
    val openingHours: List<String> = emptyList(),
    val openNow: Boolean? = null,
    val openStatusText: String? = null,
    val types: List<String> = emptyList()
)