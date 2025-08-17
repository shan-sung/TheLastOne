package com.example.thelastone.di

import com.example.thelastone.data.model.AuthUser
import com.example.thelastone.data.model.User
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

// di/SessionModule.kt
@Module
@InstallIn(SingletonComponent::class)
object SessionModule {
    @Provides @Singleton
    fun provideSessionManager(): SessionManager = SessionManager().apply { setDemoUser() }
}

@Singleton
class SessionManager @Inject constructor(
    // 之後可注入 DataStore 來持久化 token
) {
    private val _auth = MutableStateFlow<AuthUser?>(null)
    val auth: StateFlow<AuthUser?> = _auth

    val isLoggedIn: Boolean get() = _auth.value != null
    val currentUserId: String get() = _auth.value?.user?.id
        ?: error("No user. Require login.")

    fun setAuth(auth: AuthUser?) { _auth.value = auth }
    fun setDemoUser() { _auth.value = DEMO_AUTH }          // 開發期
    fun clear() { _auth.value = null }                      // 登出
}

// 一處集中定義 Demo 帳號
val DEMO_USER = User(
    id = "demo-user",
    name = "Demo User",
    email = "demo@example.com",
    avatarUrl = null,
    friends = listOf("friend-1", "friend-2")
)
val DEMO_AUTH = AuthUser(
    token = "demo-token",
    user = DEMO_USER
)