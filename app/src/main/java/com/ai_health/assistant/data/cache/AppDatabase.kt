package com.ai_health.assistant.data.cache

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * The Room database for this application.
 * This class serves as the main access point for the persisted health data,
 * providing the necessary configuration and access to DAOs.
 */
@Database(entities = [HealthCacheEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    /**
     * Provides access to the Data Access Object (DAO) for health data operations.
     *
     * A DAO (Data Access Object) is an interface that defines how to access and manipulate data in the database.
     * It is necessary because it abstracts the underlying persistence implementation, allowing the rest 
     * of the application to interact with data through simple function calls instead of writing SQL queries.
     */
    abstract fun healthCacheDao(): HealthCacheDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Returns the singleton instance of [AppDatabase].
         *
         * @param context The application context used to initialize the database.
         * @return The [AppDatabase] instance.
         */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "health_assistant_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
