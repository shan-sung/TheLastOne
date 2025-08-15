package com.example.thelastone.data.repo

import com.example.thelastone.data.model.FriendRequest
import com.example.thelastone.data.model.User

interface UserRepository {
    suspend fun getCurrentUser(): User
    suspend fun updateProfile(user: User)
    suspend fun searchUsers(query: String): List<User>
    suspend fun sendFriendRequest(toUserId: String)
    suspend fun getFriendRequests(): List<FriendRequest>
    suspend fun respondFriendRequest(requestId: String, accept: Boolean)
}