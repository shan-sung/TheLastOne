package com.example.thelastone.data.model

data class Message(
    val id: String,
    val tripId: String,
    val sender: User,
    val text: String,
    val timestamp: Long,           // 毫秒
    val isAi: Boolean = false      // 系統 AI 訊息
)
