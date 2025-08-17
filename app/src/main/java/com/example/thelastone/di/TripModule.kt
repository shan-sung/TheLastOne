package com.example.thelastone.di

import com.example.thelastone.data.fakerepo.FakeTripRepository
import com.example.thelastone.data.repo.TripRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TripModule {
    @Binds @Singleton
    abstract fun bindTripRepository(impl: FakeTripRepository): TripRepository
}
