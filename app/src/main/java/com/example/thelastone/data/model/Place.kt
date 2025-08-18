package com.example.thelastone.data.model

import kotlinx.serialization.Serializable

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

@Serializable
data class PlaceLite(
    val placeId: String,
    val name: String,
    val lat: Double,
    val lng: Double,
    val address: String? = null,
    val rating: Double? = null,
    val userRatingsTotal: Int? = null,
    val photoUrl: String? = null,
    val openingHours: List<String> = emptyList(), // 👈 新增
    val openNow: Boolean? = null,                  // 👈 可選（若要顯示「營業中」）
    val openStatusText: String? = null            // ✅ 算好的文案：「營業中 · 至 21:00」
)


// 互轉（在 PickPlace 選完 & AddActivity 要建 Activity 時用）
fun Place.toLite() = PlaceLite(placeId, name, lat, lng, address, rating, userRatingsTotal, photoUrl, openingHours)
fun PlaceLite.toFull(): Place = Place(
    placeId = placeId,
    name = name,
    rating = rating,
    userRatingsTotal = userRatingsTotal,
    address = address,
    lat = lat,
    lng = lng,
    photoUrl = photoUrl,
    openingHours = openingHours,   // 👈 帶進去
    miniMapUrl = null
)
