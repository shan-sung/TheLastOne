package com.example.thelastone.di

// di/RepositoryModule.kt
import com.example.thelastone.data.repo.MemorySavedRepository
import com.example.thelastone.data.repo.MemoryTripRepository
import com.example.thelastone.data.repo.SavedRepository
import com.example.thelastone.data.repo.TripRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideTripRepository(): TripRepository = MemoryTripRepository()

    @Provides
    @Singleton
    fun provideSavedRepository(): SavedRepository = MemorySavedRepository()
}
