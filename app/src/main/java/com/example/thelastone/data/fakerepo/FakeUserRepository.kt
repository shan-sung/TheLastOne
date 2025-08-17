package com.example.thelastone.data.fakerepo

import com.example.thelastone.data.model.AuthUser
import com.example.thelastone.data.model.User
import com.example.thelastone.data.repo.UserRepository
import com.example.thelastone.di.DEMO_USER
import com.example.thelastone.di.SessionManager
import kotlinx.coroutines.delay
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeUserRepository @Inject constructor(
    private val session: SessionManager
) : UserRepository {

    // 使用者快取
    private val usersByEmail = ConcurrentHashMap<String, User>() // key = email(lowercase)
    private val usersById    = ConcurrentHashMap<String, User>() // key = id

    // 好友關係：userId -> set(friendId)
    private val friendships  = ConcurrentHashMap<String, MutableSet<String>>()

    init {
        // 內建三個使用者
        addOrReplace(DEMO_USER)
        addOrReplace(User(id = "friend-1", name = "Alice", email = "alice@example.com"))
        addOrReplace(User(id = "friend-2", name = "Bob",   email = "bob@example.com"))

        // DemoUser 預設跟 Alice / Bob 互相是好友（雙向）
        befriend(DEMO_USER.id, "friend-1")
        befriend(DEMO_USER.id, "friend-2")
    }

    // --- UserRepository ---

    override suspend fun login(email: String, password: String): AuthUser {
        delay(200)
        val key  = email.lowercase()
        val user = usersByEmail.getOrPut(key) {
            // 首次登入的假帳號：自動建立
            val id = "u-${UUID.randomUUID()}"
            User(
                id = id,
                name = key.substringBefore("@").replaceFirstChar { it.uppercase() },
                email = email
            ).also { addOrReplace(it) }
        }
        val auth = AuthUser(token = "demo-token-${user.id}", user = user)
        session.setAuth(auth)
        // 確保好友映射存在
        friendships.putIfAbsent(user.id, ConcurrentHashMap.newKeySet())
        return auth
    }

    override suspend fun register(name: String, email: String, password: String): AuthUser {
        delay(300)
        val key = email.lowercase()
        val user = User(id = "u-${UUID.randomUUID()}", name = name, email = email)
        addOrReplace(user)
        val auth = AuthUser(token = "demo-token-${user.id}", user = user)
        session.setAuth(auth)
        friendships.putIfAbsent(user.id, ConcurrentHashMap.newKeySet())
        return auth
    }

    override suspend fun logout() {
        delay(100)
        session.clear()
    }

    override suspend fun getFriends(): List<User> {
        delay(150)
        val me = session.currentUserId
        val ids = friendships[me].orEmpty()
        return ids.mapNotNull { usersById[it] }
    }

    override suspend fun searchUsers(keyword: String): List<User> {
        delay(150)
        val q = keyword.trim()
        if (q.isEmpty()) return emptyList()
        return usersById.values.filter {
            it.name.contains(q, ignoreCase = true) || it.email.contains(q, ignoreCase = true)
        }
    }

    override suspend fun sendFriendRequest(toUserId: String) {
        delay(120)
        val from = session.currentUserId
        if (from == toUserId) return
        // 假資料：直接「自動接受」→ 形成雙向好友
        befriend(from, toUserId)
    }

    // --- helpers ---

    private fun addOrReplace(u: User) {
        usersByEmail[u.email.lowercase()] = u
        usersById[u.id] = u
        friendships.putIfAbsent(u.id, ConcurrentHashMap.newKeySet())
    }

    private fun befriend(a: String, b: String) {
        friendships.computeIfAbsent(a) { ConcurrentHashMap.newKeySet() }.add(b)
        friendships.computeIfAbsent(b) { ConcurrentHashMap.newKeySet() }.add(a)
    }
}
