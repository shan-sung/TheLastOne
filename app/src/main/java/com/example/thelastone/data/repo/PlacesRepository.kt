package com.example.thelastone.data.repo

import com.example.thelastone.data.model.PlaceLite

interface PlacesRepository {
    // suspend fun getNearbyPlaces(lat: Double, lng: Double): List<Place>
    suspend fun searchText(
        query: String,
        lat: Double? = null,
        lng: Double? = null,
        radiusMeters: Double? = null,
        openNow: Boolean? = null
    ): List<PlaceLite>
    // suspend fun getPlaceDetail(placeId: String): Place
}