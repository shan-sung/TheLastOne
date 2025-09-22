package com.example.thelastone.data.repo

import com.example.thelastone.data.model.Alternative
import com.example.thelastone.data.model.PlaceLite
import com.example.thelastone.data.model.StartInfo

interface StartRepository {
    suspend fun getStartInfo(place: PlaceLite): StartInfo   // ← 替換
    suspend fun getAlternatives(placeId: String, page: Int): List<Alternative>
}