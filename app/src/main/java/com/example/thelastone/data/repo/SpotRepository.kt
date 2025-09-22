package com.example.thelastone.data.repo

import com.example.thelastone.data.model.City
import com.example.thelastone.data.model.PlaceLite
import com.example.thelastone.data.model.SpotFilters
import javax.inject.Inject

// data/repo/SpotRepository.kt
interface SpotRepository {
    // 附近熱門（可帶座標）；若 lat/lng 為 null，實作可退回全域熱門
    suspend fun getRecommendedSpots(
        userId: String? = null,
        limit: Int = 30,
        lat: Double? = null,
        lng: Double? = null,
        radiusMeters: Int? = null,
        openNow: Boolean? = null
    ): List<PlaceLite>

    // 台灣熱門（無定位就用這個）
    suspend fun getTaiwanPopularSpots(
        userId: String? = null,
        limit: Int = 30
    ): List<PlaceLite>

    suspend fun getFilteredSpots(filters: SpotFilters, limit: Int = 30): List<PlaceLite>
}

// 之後切到後端時，改寫這個實作就好（或新增 BackendSpotRepository）。
class DefaultSpotRepository @Inject constructor(
    private val placesRepo: PlacesRepository
) : SpotRepository {

    override suspend fun getRecommendedSpots(
        userId: String?,
        limit: Int,
        lat: Double?,
        lng: Double?,
        radiusMeters: Int?,
        openNow: Boolean?
    ): List<PlaceLite> {
        // 有座標 → 以使用者附近搜尋熱門景點；沒有就退回全球熱門
        return placesRepo.searchText(
            query = "top tourist attractions",
            lat = lat, lng = lng,
            radiusMeters = radiusMeters ?: if (lat != null && lng != null) 5000 else null,
            openNow = openNow
        ).take(limit)
    }

    override suspend fun getTaiwanPopularSpots(userId: String?, limit: Int): List<PlaceLite> {
        val taiwanLat = 23.6978
        val taiwanLng = 120.9605

        return placesRepo.searchText(
            query = "top tourist attractions Taiwan",
            lat = taiwanLat,
            lng = taiwanLng,
            radiusMeters = 50_000, // ⬅️ 修正：不得超過 50,000
            openNow = null
        ).take(limit)
    }

    // data/repo/impl/DefaultSpotRepository.kt
    override suspend fun getFilteredSpots(filters: SpotFilters, limit: Int): List<PlaceLite> {
        val types = filters.categories.flatMap { it.primaryTypes }.distinct().take(5)
        val targetCities = if (filters.cities.isNotEmpty()) filters.cities else City.values().toSet()

        // 每個城市取一批，再彙整
        val perCityCap = 20                 // API nearby 上限
        val want = maxOf(limit, perCityCap) // 讓聚合後有料再過濾

        val raw = targetCities.flatMap { c ->
            placesRepo.searchNearby(
                lat = c.lat, lng = c.lng,
                radiusMeters = c.radiusMeters,
                includedTypes = if (types.isEmpty()) listOf("tourist_attraction") else types,
                rankPreference = RankPreference.POPULARITY,
                openNow = filters.openNow,
                maxResultCount = perCityCap
            )
        }

        // 本地補強（評分 / 類別 / 營業中）
        val minR = filters.minRating.toDouble()
        val maxR = filters.maxRating.toDouble()
        val required = types.toSet()
        val wantOpen = filters.openNow == true

        return raw.asSequence()
            .filter { (it.rating ?: 0.0) in minR..maxR }
            .filter { required.isEmpty() || it.types.any(required::contains) }
            .filter { !wantOpen || it.openNow == true }
            .distinctBy { it.placeId }
            .sortedWith(
                compareByDescending<PlaceLite> { it.userRatingsTotal ?: 0 }
                    .thenByDescending { it.rating ?: 0.0 }
            )
            .take(limit)
            .toList()
    }
}