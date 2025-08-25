package com.example.thelastone.data.repo

import com.example.thelastone.data.model.PlaceLite
import javax.inject.Inject

// data/repo/SpotRepository.kt
interface SpotRepository {
    suspend fun getRecommendedSpots(
        userId: String? = null,
        limit: Int = 30
    ): List<PlaceLite>
}

// 之後切到後端時，改寫這個實作就好（或新增一個 BackendSpotRepository）。
class DefaultSpotRepository @Inject constructor(
    private val placesRepo: PlacesRepository
) : SpotRepository {
    override suspend fun getRecommendedSpots(userId: String?, limit: Int): List<PlaceLite> {
        // TODO: 之後改成呼叫你們後端，例如：
        // return backendApi.getRecommendedSpots(userId, limit).map { ... }

        // 目前先用 text search 當 placeholder（全球熱門）
        return placesRepo.searchText(
            query = "top tourist attractions",
            lat = null, lng = null, radiusMeters = null, openNow = null
        ).take(limit)
    }
}