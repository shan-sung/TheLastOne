package com.example.thelastone.data.model

/**
 * 盡量貼近 Google Places 的欄位語意：
 * - rating      -> Double?（Places API 本來就是 0.0~5.0 的浮點）
 * - userRatingsTotal -> Int?
 * - lat/lng     -> Double（座標）
 * 其餘欄位維持你的需求
 */
data class Place(
    val placeId: String,
    val name: String,
    val rating: Double?,                   // << 改成 Double? 解決你的型別錯誤
    val userRatingsTotal: Int?,
    val address: String?,
    val openingHours: List<String> = emptyList(),
    val lat: Double,
    val lng: Double,
    val photoUrl: String? = null,
    val miniMapUrl: String? = null
)