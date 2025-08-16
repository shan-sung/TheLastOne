package com.example.thelastone.di

import com.example.thelastone.data.fakerepo.FakeChatService
import com.example.thelastone.data.local.MessageDao
import com.example.thelastone.data.remote.ChatService
import com.example.thelastone.data.repo.ChatRepository
import com.example.thelastone.data.repo.ChatRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ChatModule {

    @Provides @Singleton
    fun provideChatService(): ChatService = FakeChatService()

    @Provides @Singleton
    fun provideChatRepository(
        service: ChatService,
        dao: MessageDao,
        json: Json
    ): ChatRepository = ChatRepositoryImpl(service, dao, json)
}
