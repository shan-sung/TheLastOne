package com.example.thelastone.data.model

data class Message(
    val id: String,
    val tripId: String,
    val sender: User,
    val text: String,
    val timestamp: Long,           // 毫秒
    val isAi: Boolean = false,     // 系統 AI 訊息
    val suggestions: List<PlaceLite>? = null // ← AI 訊息可帶三個建議地點
)

data class AnalysisResult(
    val aiText: String,
    val suggestions: List<PlaceLite>
)