package com.example.thelastone.data.model

// 新增：平均年齡分組（單選）
enum class AgeBand {
    IGNORE, UNDER_17, A18_25, A26_35, A36_45, A46_55, A56_PLUS
}

data class TripForm(
    val name: String,
    val totalBudget: Int?,                 // 單位：新台幣（只存純數字）
    val startDate: String,                 // yyyy-MM-dd
    val endDate: String,                   // yyyy-MM-dd
    val activityStart: String?,            // "HH:mm"（選填）
    val activityEnd: String?,              // "HH:mm"（選填）
    val transportPreferences: List<String>,
    val useGmapsRating: Boolean,
    val styles: List<String>,
    val avgAge: AgeBand                    // ✅ 單選必填（含 IGNORE）
)