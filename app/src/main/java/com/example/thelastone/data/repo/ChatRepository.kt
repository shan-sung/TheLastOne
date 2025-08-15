package com.example.thelastone.data.repo

import com.example.thelastone.data.model.Message
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun observeMessages(tripId: String): Flow<List<Message>>
    suspend fun sendMessage(tripId: String, message: String)
}