package com.example.thelastone.data.repo

import com.example.thelastone.data.model.AuthUser
import com.example.thelastone.data.model.FriendRequest
import com.example.thelastone.data.model.User

interface UserRepository {
    suspend fun login(email: String, password: String): AuthUser
    suspend fun register(name: String, email: String, password: String): AuthUser
    suspend fun logout()

    // 朋友/搜尋
    suspend fun getFriends(): List<User>                 // 以目前登入者為準
    suspend fun searchUsers(keyword: String): List<User>
    suspend fun sendFriendRequest(toUserId: String)

    // ★ 新增：處理好友邀請（別人寄來）
    suspend fun getIncomingFriendRequests(): List<FriendRequest>
    suspend fun acceptFriendRequest(requestId: String)
    suspend fun rejectFriendRequest(requestId: String)

    // （選用）查單一使用者
    suspend fun getUserById(userId: String): User?
}
