package com.example.thelastone.data.remote

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface PlacesApi {
    @POST("v1/places:searchText")
    suspend fun searchText(
        @Body body: SearchTextBody,
        @Header("X-Goog-Api-Key") apiKey: String,
        @Header("X-Goog-FieldMask") fieldMask: String =
            listOf(
                "places.id","places.displayName","places.formattedAddress","places.location",
                "places.rating","places.userRatingCount","places.photos.name",
                "places.businessStatus","places.utcOffsetMinutes",
                "places.currentOpeningHours.openNow",
                "places.currentOpeningHours.periods",
                "places.currentOpeningHours.weekdayDescriptions",
                "places.regularOpeningHours.periods"
            ).joinToString(",")

    ): SearchTextResponse
}


/** ---- DTO ---- */
@Serializable
data class SearchTextBody(
    val textQuery: String,
    val locationBias: LocationBias? = null,
    val openNow: Boolean? = null,
    val languageCode: String? = null,   // ✅ 新增：指定回傳語言
    val regionCode: String? = null
)

@Serializable data class LocationBias(val circle: Circle? = null)
@Serializable data class Circle(val center: LatLng, val radius: Double)
@Serializable data class LatLng(val latitude: Double, val longitude: Double)

@Serializable data class SearchTextResponse(val places: List<ApiPlace>?)
@Serializable
data class ApiPlace(
    val id: String?,
    val displayName: TextVal?,
    val formattedAddress: String?,
    val shortFormattedAddress: String? = null,
    val location: LatLng?,
    val rating: Double? = null,
    val userRatingCount: Int? = null,
    val photos: List<ApiPhoto>? = null,
    val businessStatus: String? = null,
    val utcOffsetMinutes: Int? = null,          // ✅ 要有
    val currentOpeningHours: ApiOpeningHours? = null,
    val regularOpeningHours: ApiOpeningHours? = null // ✅ 要有
)
@Serializable data class ApiPhoto(val name: String?)
@Serializable data class TextVal(val text: String?)

@Serializable
data class ApiOpeningHours(
    val openNow: Boolean? = null,
    val weekdayDescriptions: List<String>? = null,
    val periods: List<ApiPeriod>? = null        // ← 確保有 periods
)

@Serializable
data class ApiPeriod(
    val open: ApiPoint? = null,
    val close: ApiPoint? = null
)
@Serializable
data class ApiPoint(
    val day: Int? = null,   // ✅ 由 String? 改為 Int?
    val hour: Int? = null,
    val minute: Int? = null
)