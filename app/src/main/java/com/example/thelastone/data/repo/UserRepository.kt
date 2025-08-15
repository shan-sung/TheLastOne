package com.example.thelastone.data.repo

import com.example.thelastone.data.model.AuthUser
import com.example.thelastone.data.model.User

interface UserRepository {
    suspend fun login(email: String, password: String): AuthUser
    suspend fun register(name: String, email: String, password: String): AuthUser
    suspend fun getFriends(userId: String): List<User>
    suspend fun searchUsers(keyword: String): List<User>
    suspend fun sendFriendRequest(fromUserId: String, toUserId: String)
}