package com.example.thelastone.di

import com.example.thelastone.data.fakerepo.FakeTripRepository
import com.example.thelastone.data.repo.TripRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TripModule {
    @Provides
    @Singleton
    fun provideTripRepository(): TripRepository = FakeTripRepository()
}
