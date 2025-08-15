package com.example.thelastone.data.repo

import com.example.thelastone.data.model.Activity
import com.example.thelastone.data.model.DateRange
import com.example.thelastone.data.model.DaySchedule
import com.example.thelastone.data.model.Trip
import com.example.thelastone.data.model.TripForm

class MemoryTripRepository : TripRepository {
    private val trips = mutableListOf<Trip>()

    override suspend fun createTrip(form: TripForm): Trip {
        val tripId = System.currentTimeMillis().toString()
        val days = generateDays(form.dateRange)
        val trip = Trip(
            id = tripId,
            name = form.name,
            ownerId = "user_1",
            budget = form.budget,
            dateRange = form.dateRange,
            transportPreferences = form.transportPreferences,
            useGmapsRating = form.useGmapsRating,
            styles = form.styles,
            schedule = days.toMutableList(),
            createdAt = System.currentTimeMillis()
        )
        trips.add(trip)
        return trip
    }

    override suspend fun getMyTrips(): List<Trip> {
        return trips
    }

    override suspend fun getTrip(tripId: String): Trip {
        return trips.find { it.id == tripId }
            ?: throw NoSuchElementException("Trip not found: $tripId")
    }

    override suspend fun addActivity(tripId: String, dayIndex: Int, activity: Activity) {
        val trip = getTrip(tripId) // 直接用上面保證非 null 的方法
        if (dayIndex in trip.schedule.indices) {
            trip.schedule[dayIndex].activities.add(activity)
        }
    }

    override suspend fun inviteFriend(tripId: String, friendId: String) {
        // Memory 版只是模擬，不做事
        println("Inviting friend $friendId to trip $tripId (MemoryRepo)")
    }

    private fun generateDays(dateRange: DateRange): List<DaySchedule> {
        // 假資料，只放開始與結束兩天
        return listOf(
            DaySchedule(date = dateRange.start),
            DaySchedule(date = dateRange.end)
        )
    }
}

