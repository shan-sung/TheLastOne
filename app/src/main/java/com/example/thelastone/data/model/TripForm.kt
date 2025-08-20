package com.example.thelastone.data.model

// 新增：平均年齡分組（單選）
enum class AgeBand {
    IGNORE, UNDER_17, A18_25, A26_35, A36_45, A46_55, A56_PLUS
}

data class TripForm(
    val name: String,
    val totalBudget: Int?,
    val startDate: String,
    val endDate: String,
    val activityStart: String?,
    val activityEnd: String?,
    val transportPreferences: List<String>,
    val useGmapsRating: Boolean,
    val styles: List<String>,
    val avgAge: AgeBand,
    val visibility: TripVisibility = TripVisibility.PRIVATE     // ← 新增
)