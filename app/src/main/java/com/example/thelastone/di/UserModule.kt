// di/UserModule.kt
package com.example.thelastone.di

import com.example.thelastone.data.local.SavedPlaceDao
import com.example.thelastone.data.repo.DefaultSpotRepository
import com.example.thelastone.data.repo.PlacesRepository
import com.example.thelastone.data.repo.SavedRepository
import com.example.thelastone.data.repo.SpotRepository
import com.example.thelastone.data.repo.impl.fake.FakeUserRepository
import com.example.thelastone.data.repo.UserRepository
import com.example.thelastone.data.repo.impl.SavedRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class UserModule {

    @Binds
    @Singleton
    abstract fun bindUserRepository(
        impl: FakeUserRepository
    ): UserRepository
}

// di/RepoModule.kt
@Module
@InstallIn(SingletonComponent::class)
object RepoModule {

    @Provides @Singleton
    fun provideSavedRepository(
        dao: SavedPlaceDao
    ): SavedRepository = SavedRepositoryImpl(dao)

    // ★ NEW: SpotRepository 綁定（目前用 DefaultSpotRepository 包 PlacesRepository）
    @Provides @Singleton
    fun provideSpotRepository(
        placesRepo: PlacesRepository
    ): SpotRepository = DefaultSpotRepository(placesRepo)
}

