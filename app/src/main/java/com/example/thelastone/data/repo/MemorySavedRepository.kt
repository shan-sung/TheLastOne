package com.example.thelastone.data.repo

import com.example.thelastone.data.model.SavedPlace

// repositories/MemorySavedRepository.kt
class MemorySavedRepository : SavedRepository {
    private val savedPlaces = mutableListOf<SavedPlace>()

    override suspend fun savePlace(place: SavedPlace) {
        if (savedPlaces.none { it.placeId == place.placeId }) {
            savedPlaces.add(place)
        }
    }

    override suspend fun removePlace(placeId: String) {
        savedPlaces.removeAll { it.placeId == placeId }
    }

    override suspend fun getSavedPlaces(): List<SavedPlace> {
        return savedPlaces
    }
}
