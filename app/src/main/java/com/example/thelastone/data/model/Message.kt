package com.example.thelastone.data.model

data class Message(
    val id: String,                    // MongoDB _id
    val tripId: String,
    val senderId: String,
    val text: String,
    val timestamp: Long,
    val isAI: Boolean = false
)
