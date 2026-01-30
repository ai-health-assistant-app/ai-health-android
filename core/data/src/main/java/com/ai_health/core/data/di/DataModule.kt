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

    @Binds
    @Singleton
    abstract fun bindActivityLogRepository(
        activityLogRepositoryImpl: com.ai_health.core.data.repository.ActivityLogRepositoryImpl
    ): com.ai_health.core.domain.repository.ActivityLogRepository
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
    fun provideActivityLogDao(appDatabase: AppDatabase): com.ai_health.core.data.local.dao.ActivityLogDao {
        return appDatabase.activityLogDao()
    }
}
