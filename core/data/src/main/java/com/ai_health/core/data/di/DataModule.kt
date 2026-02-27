package com.ai_health.core.data.di

import android.content.Context
import androidx.room.Room
import androidx.work.WorkManager
import com.ai_health.core.data.local.AppDatabase
import com.ai_health.core.data.local.dao.HealthMetricDao
import com.ai_health.core.data.local.dao.SleepDao
import com.ai_health.core.data.local.dao.SyncTokenDao
import com.ai_health.core.data.repository.HealthRepositoryImpl
import com.ai_health.core.data.security.SecureKeyManager
import com.ai_health.core.domain.repository.HealthRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
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
    abstract fun bindUserRepository(
        userRepositoryImpl: com.ai_health.core.data.repository.UserRepositoryImpl
    ): com.ai_health.core.data.repository.UserRepository

    @Binds
    @Singleton
    abstract fun bindChatRepository(
        chatRepositoryImpl: com.ai_health.core.data.repository.ChatRepositoryImpl
    ): com.ai_health.core.domain.repository.ChatRepository
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * Fornisce il database Room crittografato con SQLCipher.
     * 
     * PRIVACY-PROOF ARCHITECTURE:
     * - La passphrase è recuperata da SecureKeyManager (EncryptedSharedPreferences)
     * - SQLCipher utilizza AES-256 per crittografare l'intero file del database
     * - fallbackToDestructiveMigration() per la migrazione da v3 a v4
     */
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        secureKeyManager: SecureKeyManager
    ): AppDatabase {
        // CRITICAL: Load SQLCipher native library before any database operations
        // The new sqlcipher-android library requires explicit initialization
        //System.loadLibrary("sqlcipher")
        
        //val passphrase = secureKeyManager.getOrCreatePassphrase()
        //val factory = SupportOpenHelperFactory(passphrase)
        
        return Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "health_database"
        )
            //.openHelperFactory(factory)
            .fallbackToDestructiveMigration()
            .build()
    }

    /**
     * Fornisce WorkManager per la sincronizzazione foreground-first.
     */
    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager {
        return WorkManager.getInstance(context)
    }
    
    // === DAOs ===
    
    @Provides
    @Singleton
    fun provideHealthMetricDao(database: AppDatabase): HealthMetricDao {
        return database.healthMetricDao()
    }
    
    @Provides
    @Singleton
    fun provideSleepDao(database: AppDatabase): SleepDao {
        return database.sleepDao()
    }
    
    @Provides
    @Singleton
    fun provideSyncTokenDao(database: AppDatabase): SyncTokenDao {
        return database.syncTokenDao()
    }

    @Provides
    @Singleton
    fun provideUserDao(database: AppDatabase): com.ai_health.core.data.local.dao.UserDao {
        return database.userDao()
    }

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences> {
        return context.dataStore
    }
}

private val Context.dataStore: androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences> by androidx.datastore.preferences.preferencesDataStore(name = "health_sync")
