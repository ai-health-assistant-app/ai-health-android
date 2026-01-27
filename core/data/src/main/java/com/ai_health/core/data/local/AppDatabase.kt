package com.ai_health.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.ai_health.core.data.local.converter.Converters
import com.ai_health.core.data.local.dao.HealthMetricDao
import com.ai_health.core.data.local.dao.SleepDao
import com.ai_health.core.data.local.entity.BasalMetabolicRateEntity
import com.ai_health.core.data.local.entity.CaloriesEntity
import com.ai_health.core.data.local.entity.DistanceEntity
import com.ai_health.core.data.local.entity.ExerciseSessionEntity
import com.ai_health.core.data.local.entity.HeartRateEntity
import com.ai_health.core.data.local.entity.OxygenSaturationEntity
import com.ai_health.core.data.local.entity.SleepSessionEntity
import com.ai_health.core.data.local.entity.SleepStageEntity
import com.ai_health.core.data.local.entity.StepsEntity

@Database(
    entities = [
        HeartRateEntity::class,
        StepsEntity::class,
        ExerciseSessionEntity::class,
        OxygenSaturationEntity::class,
        DistanceEntity::class,
        CaloriesEntity::class,
        BasalMetabolicRateEntity::class,
        SleepSessionEntity::class,
        SleepStageEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun healthMetricDao(): HealthMetricDao
    abstract fun sleepDao(): SleepDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: android.content.Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "health_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
