package com.example.thelastone.data.repo.impl

import com.example.thelastone.BuildConfig
import com.example.thelastone.data.model.PlaceLite
import com.example.thelastone.data.remote.ApiPlace
import com.example.thelastone.data.remote.Circle
import com.example.thelastone.data.remote.LatLng
import com.example.thelastone.data.remote.LocationBias
import com.example.thelastone.data.remote.LocationRestriction
import com.example.thelastone.data.remote.PlacesApi
import com.example.thelastone.data.remote.SearchNearbyBody
import com.example.thelastone.data.remote.SearchTextBody
import com.example.thelastone.data.repo.PlacesRepository
import com.example.thelastone.utils.buildOpenStatus
import com.example.thelastone.utils.stripCountryTaiwanPrefix
import com.example.thelastone.utils.stripPostalCodeIfAny
import javax.inject.Inject

class PlacesRepositoryImpl @Inject constructor(
    private val api: PlacesApi
) : PlacesRepository {

    override suspend fun searchText(
        query: String,
        lat: Double?, lng: Double?, radiusMeters: Double?, openNow: Boolean?
    ): List<PlaceLite> {
        val bias =
            if (lat != null && lng != null && radiusMeters != null)
                LocationBias(circle = Circle(center = LatLng(lat, lng), radius = radiusMeters))
            else null

        val resp = api.searchText(
            SearchTextBody(
                textQuery = query,
                locationBias = bias,
                openNow = openNow,
                languageCode = "zh-TW",
                regionCode = "TW"
            )
        )

        return mapApiPlacesToLite(resp.places)
    }

    override suspend fun searchNearby(
        lat: Double,
        lng: Double,
        radiusMeters: Double,
        includedTypes: List<String>,
        rankPreference: String,
        openNow: Boolean?,
        maxResultCount: Int
    ): List<PlaceLite> {
        val resp = api.searchNearby(
            SearchNearbyBody(
                locationRestriction = LocationRestriction(
                    circle = Circle(center = LatLng(lat, lng), radius = radiusMeters)
                ),
                includedTypes = includedTypes,
                maxResultCount = maxResultCount.coerceIn(1, 20),
                openNow = openNow,
                rankPreference = rankPreference,   // "POPULARITY" 或 "DISTANCE"
                languageCode = "zh-TW",
                regionCode = "TW"
            )
        )
        return mapApiPlacesToLite(resp.places)
    }

    // 共用轉換
    private fun mapApiPlacesToLite(list: List<ApiPlace>?): List<PlaceLite> =
        list.orEmpty().mapNotNull { p ->
            val id = p.id?.substringAfter("places/") ?: return@mapNotNull null
            val name = p.displayName?.text ?: "未命名地點"
            val la = p.location?.latitude ?: 0.0
            val lo = p.location?.longitude ?: 0.0

            val photoName = p.photos?.firstOrNull()?.name
            val photoUrl = photoName?.let {
                "https://places.googleapis.com/v1/$it/media?maxWidthPx=400&key=${BuildConfig.MAPS_API_KEY}"
            }

            val addr = p.formattedAddress
                ?.let { stripPostalCodeIfAny(it) }
                ?.let { stripCountryTaiwanPrefix(it) }

            val status = buildOpenStatus(
                current = p.currentOpeningHours,
                regular = p.regularOpeningHours,
                utcOffsetMinutes = p.utcOffsetMinutes ?: 0
            )

            PlaceLite(
                placeId = id,
                name = name,
                lat = la,
                lng = lo,
                address = addr,
                rating = p.rating,
                userRatingsTotal = p.userRatingCount,
                photoUrl = photoUrl,
                openingHours = p.currentOpeningHours?.weekdayDescriptions ?: emptyList(),
                openNow = status?.openNow ?: p.currentOpeningHours?.openNow,
                openStatusText = status?.text
            )
        }

}
