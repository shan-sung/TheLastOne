package com.example.thelastone.data.repo

import com.example.thelastone.data.model.Activity
import com.example.thelastone.data.model.Trip
import com.example.thelastone.data.model.TripForm
import kotlinx.coroutines.flow.Flow

// CHANGED: TripRepository.kt
interface TripRepository {
    suspend fun createTrip(form: TripForm): Trip
    suspend fun saveTrip(trip: Trip): Trip

    // 原來「看自己的」
    suspend fun getMyTrips(): List<Trip>
    fun observeMyTrips(): Flow<List<Trip>>

    // NEW：看「公開」的（Explore 會用這個）
    suspend fun getPublicTrips(): List<Trip>
    fun observePublicTrips(): Flow<List<Trip>>

    suspend fun getTripDetail(tripId: String): Trip
    fun observeTripDetail(tripId: String): Flow<Trip>
    suspend fun addActivity(tripId: String, dayIndex: Int, activity: Activity)
    suspend fun updateActivity(tripId: String, dayIndex: Int, activityIndex: Int, updated: Activity)
    suspend fun removeActivity(tripId: String, dayIndex: Int, activityIndex: Int)
    suspend fun deleteTrip(tripId: String)
    suspend fun addMembers(tripId: String, userIds: List<String>)
}

