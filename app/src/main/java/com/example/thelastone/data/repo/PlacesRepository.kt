package com.example.thelastone.data.repo

import com.example.thelastone.data.model.SavedPlace

interface PlacesRepository {
    suspend fun getNearbyPlaces(lat: Double, lng: Double): List<SavedPlace>
    suspend fun searchPlaces(query: String): List<SavedPlace>
}