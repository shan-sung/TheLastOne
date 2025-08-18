package com.example.thelastone.data.repo.impl

import com.example.thelastone.data.local.SavedPlaceDao
import com.example.thelastone.data.local.SavedPlaceEntity
import com.example.thelastone.data.model.Place
import com.example.thelastone.data.model.PlaceLite
import com.example.thelastone.data.model.SavedPlace
import com.example.thelastone.data.repo.SavedRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class SavedRepositoryImpl @Inject constructor(
    private val dao: SavedPlaceDao
) : SavedRepository {

    override fun observeIds(): Flow<Set<String>> =
        dao.observeIds().map { it.toSet() }

    override fun observeAll(): Flow<List<SavedPlace>> =
        dao.observeAll().map { list ->
            list.map { e ->
                SavedPlace(
                    id = e.placeId,
                    userId = "local", // 目前單使用者；未來換成真正 userId
                    place = Place(
                        placeId = e.placeId,
                        name = e.name,
                        rating = e.rating,
                        userRatingsTotal = e.userRatingsTotal,
                        address = e.address,
                        lat = e.lat,
                        lng = e.lng,
                        photoUrl = e.photoUrl,
                        miniMapUrl = null,
                        openingHours = emptyList()
                    ),
                    savedAt = e.savedAt
                )
            }
        }

    override suspend fun save(place: PlaceLite) {
        val entity = SavedPlaceEntity(
            placeId = place.placeId,
            name = place.name,
            address = place.address,
            lat = place.lat,
            lng = place.lng,
            rating = place.rating,
            userRatingsTotal = place.userRatingsTotal,
            photoUrl = place.photoUrl
        )
        dao.upsert(entity)
    }

    override suspend fun unsave(placeId: String) {
        dao.delete(placeId)
    }

    override suspend fun toggle(place: PlaceLite) {
        val currentIds = observeIds()  // Flow -> snapshot 不方便；改兩階段簡化如下：
        // 簡單化：先嘗試刪除，若受影響 0 筆，再 upsert
        // Room Dao 不回傳刪除數，若需要可改寫 @Query 回傳 Int；這裡直接查 ids 判斷
        // （避免多次 DB 觀察，這裡再查一次最穩）
        val isSaved = dao.observeIds().map { it.contains(place.placeId) }.first()
        if (isSaved) dao.delete(place.placeId) else save(place)
    }
}
