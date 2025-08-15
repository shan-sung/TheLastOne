package com.example.thelastone.data.model

data class User(
    val id: String,               // 後端生成的唯一 ID
    val name: String,
    val email: String,
    val avatarUrl: String? = null,
    val friends: List<String> = emptyList() // 好友的 userId 列表
)