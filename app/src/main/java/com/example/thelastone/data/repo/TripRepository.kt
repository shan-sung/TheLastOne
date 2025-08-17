package com.example.thelastone.data.repo

import com.example.thelastone.data.model.Activity
import com.example.thelastone.data.model.Trip
import com.example.thelastone.data.model.TripForm
import kotlinx.coroutines.flow.Flow

interface TripRepository {
    suspend fun createTrip(form: TripForm): Trip              // ← 不帶 createdBy
    suspend fun saveTrip(trip: Trip): Trip                    // ← 不帶 createdBy
    suspend fun getMyTrips(): List<Trip>                      // ← 不帶 userId
    fun observeMyTrips(): Flow<List<Trip>>                    // ← 不帶 userId
    suspend fun getTripDetail(tripId: String): Trip
    fun observeTripDetail(tripId: String): Flow<Trip>
    suspend fun addActivity(tripId: String, dayIndex: Int, activity: Activity)
    suspend fun updateActivity(tripId: String, dayIndex: Int, activityIndex: Int, updated: Activity)
    suspend fun removeActivity(tripId: String, dayIndex: Int, activityIndex: Int)
    suspend fun deleteTrip(tripId: String)
}
