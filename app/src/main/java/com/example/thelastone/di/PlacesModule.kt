package com.example.thelastone.di

import com.example.thelastone.BuildConfig
import com.example.thelastone.data.remote.PlacesApi
import com.example.thelastone.data.repo.PlacesRepository
import com.example.thelastone.data.repo.impl.PlacesRepositoryImpl
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import javax.inject.Singleton

// PlacesModule.kt
@Module
@InstallIn(SingletonComponent::class)
object PlacesModule {
    @Provides @Singleton
    fun provideOkHttp(): OkHttpClient =
        OkHttpClient.Builder()
            .callTimeout(java.time.Duration.ofSeconds(30))
            .connectTimeout(java.time.Duration.ofSeconds(15))
            .readTimeout(java.time.Duration.ofSeconds(30))
            .addInterceptor { chain ->
                val req = chain.request().newBuilder()
                    .addHeader("X-Goog-Api-Key", BuildConfig.MAPS_API_KEY)
                    .addHeader("Accept", "application/json")
                    .build()
                chain.proceed(req)
            }
            .apply {
                if (BuildConfig.DEBUG) {
                    val logging = okhttp3.logging.HttpLoggingInterceptor().apply {
                        level = okhttp3.logging.HttpLoggingInterceptor.Level.BODY
                    }
                    addInterceptor(logging)
                }
            }
            .build()

    @Provides @Singleton
    fun provideRetrofit(okHttp: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://places.googleapis.com/")
            .client(okHttp)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides @Singleton
    fun providePlacesApi(retrofit: Retrofit): PlacesApi =
        retrofit.create(PlacesApi::class.java)

    @Provides @Singleton
    fun providePlacesRepository(api: PlacesApi): PlacesRepository =
        PlacesRepositoryImpl(api)
}
