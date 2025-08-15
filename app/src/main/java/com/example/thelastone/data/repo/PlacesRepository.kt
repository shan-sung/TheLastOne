package com.example.thelastone.data.repo

import com.example.thelastone.data.model.Place

interface PlacesRepository {
    suspend fun getNearbyPlaces(lat: Double, lng: Double): List<Place>
    suspend fun searchPlaces(query: String): List<Place>
    suspend fun getPlaceDetail(placeId: String): Place
}