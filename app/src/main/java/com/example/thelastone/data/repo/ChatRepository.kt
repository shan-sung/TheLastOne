package com.example.thelastone.data.repo

import com.example.thelastone.data.model.Message
import com.example.thelastone.data.model.User
import kotlinx.coroutines.flow.Flow

// data/repo/ChatRepository.kt
interface ChatRepository {
    fun observeMessages(tripId: String): Flow<List<Message>>
    suspend fun refresh(tripId: String)   // 進聊天室時呼叫：從雲端拉歷史→寫 Room
    suspend fun send(tripId: String, text: String, me: User)
    suspend fun analyze(tripId: String)   // 從 Room 讀歷史→丟後端→把 AI 訊息寫回 Room
}
