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
    @Provides
    @Singleton
    fun provideSessionManager(): SessionManager = SessionManager().apply { setDemoUser() }
}

@Singleton
class SessionManager @Inject constructor() {

    private val _auth = MutableStateFlow<AuthUser?>(DEMO_AUTH) // ← 預設就登入 demo
    val auth: StateFlow<AuthUser?> = _auth

    val currentUserId: String get() = _auth.value?.user?.id ?: DEMO_USER.id

    fun setDemoUser() { _auth.value = DEMO_AUTH }
    fun setAuth(authUser: AuthUser?) { _auth.value = authUser } // 之後串真登入用
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