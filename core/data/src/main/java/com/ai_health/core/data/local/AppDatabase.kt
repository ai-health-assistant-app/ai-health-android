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
import kotlinx.coroutines.launch

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
        SleepStageEntity::class,
        com.ai_health.core.data.local.entity.UserActivityEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun healthMetricDao(): HealthMetricDao
    abstract fun sleepDao(): SleepDao
    abstract fun activityLogDao(): com.ai_health.core.data.local.dao.ActivityLogDao

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
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Pre-populate data on creation
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                            // Fix: Use INSTANCE directly to avoid recursive getDatabase() call which might cause issues or deadlock risk.
                            // INSTANCE is guaranteed to be set if we are in a DAO operation that triggered onCreate.
                            INSTANCE?.let { instance ->
                                com.ai_health.core.data.local.util.RoomPrepopulator.prepopulate(
                                    context.applicationContext,
                                    instance
                                )
                            }
                        }
                    }
                })
                // .fallbackToDestructiveMigration() // DISABLED to prevent data loss on schema change without migration
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
