package com.example.thelastone.data.repo

import com.example.thelastone.data.model.Place
import com.example.thelastone.data.model.SavedPlace

interface SavedRepository {
    suspend fun getSavedPlaces(userId: String): List<SavedPlace>
    suspend fun addSavedPlace(userId: String, place: Place)
    suspend fun removeSavedPlace(userId: String, placeId: String)
}