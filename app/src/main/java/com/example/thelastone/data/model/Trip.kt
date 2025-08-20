package com.example.thelastone.data.model

data class Trip(
    val id: String,
    val createdBy: String,
    val name: String,
    val totalBudget: Int?,
    val startDate: String,
    val endDate: String,
    val activityStart: String?, // 新增
    val activityEnd: String?, // 新增
    val avgAge: AgeBand, // 新增
    val transportPreferences: List<String>,
    val useGmapsRating: Boolean,
    val styles: List<String>,
    val members: List<User> = emptyList(),
    val days: List<DaySchedule> = emptyList()
)

data class DaySchedule(
    val date: String,              // "yyyy-MM-dd"
    val activities: List<Activity> = emptyList()
)

data class Activity(
    val id: String,                 // 後端生成或 UUID
    val place: Place,
    val startTime: String? = null,  // "09:00"
    val endTime: String? = null,    // "11:30"
    val note: String? = null
)