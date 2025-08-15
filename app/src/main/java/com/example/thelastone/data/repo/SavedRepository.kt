package com.example.thelastone.data.repo

import com.example.thelastone.data.model.SavedPlace

interface SavedRepository {
    suspend fun savePlace(place: SavedPlace)
    suspend fun removePlace(placeId: String)
    suspend fun getSavedPlaces(): List<SavedPlace>
}