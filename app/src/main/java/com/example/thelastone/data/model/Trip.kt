package com.example.thelastone.data.model

data class Trip(
    val id: String,                  // MongoDB _id
    val name: String,
    val ownerId: String,              // 建立者
    val memberIds: List<String> = emptyList(), // 參加者
    val budget: Int?,                 // 總預算
    val dateRange: DateRange,
    val transportPreferences: List<String> = emptyList(), // ["Car", "Bus", "Walk"...]
    val useGmapsRating: Boolean,
    val styles: List<String> = emptyList(),               // 旅遊風格
    val schedule: List<DaySchedule> = emptyList(),        // AI生成或後續編輯
    val createdAt: Long
)

data class DateRange(
    val start: String,  // ISO "YYYY-MM-DD"
    val end: String
)

data class DaySchedule(
    val date: String,
    val activities: MutableList<Activity> = mutableListOf()
)

data class Activity(
    val placeId: String,
    val name: String,
    val lat: Double,
    val lng: Double,
    val startTime: String? = null,     // "HH:mm"
    val endTime: String? = null,
    val note: String? = null,          // 備註
    val rating: Double? = null,
    val userRatingsTotal: Int? = null,
    val address: String? = null
)
