package com.example.thelastone.data.model

data class FriendRequest(
    val id: String,            // MongoDB _id
    val fromUserId: String,
    val toUserId: String,
    val status: String,        // "pending", "accepted", "rejected"
    val createdAt: Long
)
