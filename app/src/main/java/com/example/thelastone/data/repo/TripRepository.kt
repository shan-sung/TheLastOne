package com.example.thelastone.data.repo

import com.example.thelastone.data.model.Activity
import com.example.thelastone.data.model.Trip
import com.example.thelastone.data.model.TripForm

interface TripRepository {
    suspend fun createTrip(form: TripForm): Trip
    suspend fun getMyTrips(): List<Trip>
    suspend fun getTrip(tripId: String): Trip
    suspend fun addActivity(tripId: String, dayIndex: Int, activity: Activity)
    suspend fun inviteFriend(tripId: String, friendId: String)
}