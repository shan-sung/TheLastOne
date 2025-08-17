package com.example.thelastone.data.repo

import com.example.thelastone.data.model.AuthUser
import com.example.thelastone.data.model.User

interface UserRepository {
    suspend fun login(email: String, password: String): AuthUser
    suspend fun register(name: String, email: String, password: String): AuthUser
    suspend fun logout()
    suspend fun getFriends(): List<User>             // ← 不帶 userId
    suspend fun searchUsers(keyword: String): List<User>
    suspend fun sendFriendRequest(toUserId: String)  // ← 不帶 fromUserId
}