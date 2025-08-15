package com.example.thelastone.data.model

data class User(
    val id: String,              // MongoDB _id (ObjectId)
    val email: String,
    val name: String,
    val avatarUrl: String? = null,
    val friends: List<String> = emptyList(), // 儲存 friend 的 userId
    val savedPlaces: List<SavedPlace> = emptyList()
)
