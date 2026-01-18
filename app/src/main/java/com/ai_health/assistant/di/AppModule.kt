package com.ai_health.assistant.di

import android.content.Context
import com.ai_health.core.data.local.AppDatabase
import com.ai_health.core.domain.repository.HealthRepository
import com.ai_health.core.health.HealthConnectManager
import com.ai_health.core.data.repository.HealthRepositoryImpl

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class) // Questi oggetti vivono finché l'app è aperta
object AppModule {

    @Provides
    @Singleton
    fun provideHealthConnectManager(@ApplicationContext context: Context): HealthConnectManager {
        return HealthConnectManager(context)
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideHealthRepository(impl: HealthRepositoryImpl): HealthRepository {
        return impl
    }
}