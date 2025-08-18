package com.example.thelastone.data.repo

import com.example.thelastone.data.model.PlaceLite

// data/repo/PlacesRepository.kt
interface PlacesRepository {
    suspend fun searchText(
        query: String,
        lat: Double? = null,
        lng: Double? = null,
        radiusMeters: Double? = null,
        openNow: Boolean? = null
    ): List<PlaceLite>

    suspend fun searchNearby(
        lat: Double,
        lng: Double,
        radiusMeters: Double,
        includedTypes: List<String> = listOf("tourist_attraction"), // 預設找景點
        rankPreference: String = "POPULARITY", // or "DISTANCE"
        openNow: Boolean? = null,
        maxResultCount: Int = 20
    ): List<PlaceLite>
}
