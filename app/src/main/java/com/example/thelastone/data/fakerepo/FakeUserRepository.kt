package com.example.thelastone.data.fakerepo

import com.example.thelastone.data.model.AuthUser
import com.example.thelastone.data.model.User
import com.example.thelastone.data.repo.UserRepository
import com.example.thelastone.di.DEMO_AUTH
import com.example.thelastone.di.DEMO_USER
import com.example.thelastone.di.SessionManager
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

// data/FakeUserRepository.kt
@Singleton
class FakeUserRepository @Inject constructor(
    private val session: SessionManager
) : UserRepository {

    // 假好友清單
    private val allUsers = listOf(
        DEMO_USER,
        User(
            id = "friend-1",
            name = "Alice",
            email = "alice@example.com",
            avatarUrl = null
        ),
        User(
            id = "friend-2",
            name = "Bob",
            email = "bob@example.com",
            avatarUrl = null
        )
    )

    override suspend fun login(email: String, password: String): AuthUser {
        delay(200)
        // 一律登入 DEMO_USER
        session.setDemoUser()
        return DEMO_AUTH
    }

    override suspend fun register(name: String, email: String, password: String): AuthUser {
        delay(300)
        // 不管註冊誰，都當成 Demo User
        session.setDemoUser()
        return DEMO_AUTH
    }

    override suspend fun getFriends(userId: String): List<User> {
        delay(150)
        // DEMO_USER 預設有 friend-1, friend-2
        return allUsers.filter { it.id in DEMO_USER.friends }
    }

    override suspend fun searchUsers(keyword: String): List<User> {
        delay(150)
        return allUsers.filter {
            it.name.contains(keyword, ignoreCase = true) ||
                    it.email.contains(keyword, ignoreCase = true)
        }
    }

    override suspend fun sendFriendRequest(fromUserId: String, toUserId: String) {
        delay(120)
        println("Fake sendFriendRequest: $fromUserId -> $toUserId")
        // 假動作，不做任何實際更新
    }
}
