package com.ai_health.core.data.di

import android.content.Context
import com.ai_health.core.data.local.AppDatabase
import com.ai_health.core.data.repository.HealthRepositoryImpl
import com.ai_health.core.domain.repository.HealthRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindHealthRepository(
        healthRepositoryImpl: HealthRepositoryImpl
    ): HealthRepository
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences> {
        return context.dataStore
    }
}

private val Context.dataStore: androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences> by androidx.datastore.preferences.preferencesDataStore(name = "health_sync")
