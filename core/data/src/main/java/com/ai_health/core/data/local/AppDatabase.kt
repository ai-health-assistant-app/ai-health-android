package com.ai_health.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.ai_health.core.data.local.converter.Converters
import com.ai_health.core.data.local.dao.HealthMetricDao
import com.ai_health.core.data.local.dao.SleepDao
import com.ai_health.core.data.local.dao.SyncTokenDao
import com.ai_health.core.data.local.entity.BasalMetabolicRateEntity
import com.ai_health.core.data.local.entity.CaloriesEntity
import com.ai_health.core.data.local.entity.DistanceEntity
import com.ai_health.core.data.local.entity.ExerciseSessionEntity

import com.ai_health.core.data.local.entity.HeartRateSessionEntity
import com.ai_health.core.data.local.entity.OxygenSaturationEntity
import com.ai_health.core.data.local.entity.SleepSessionEntity
import com.ai_health.core.data.local.entity.SleepStageEntity
import com.ai_health.core.data.local.entity.StepsEntity
import com.ai_health.core.data.local.entity.SyncTokenEntity

/**
 * AppDatabase - Database Room crittografato con SQLCipher.
 * 
 * PRIVACY-PROOF ARCHITECTURE:
 * - Crittografia AES-256 tramite SQLCipher
 * - Passphrase gestita da EncryptedSharedPreferences (SecureKeyManager)
 * - Nessun cloud backend, tutti i dati rimangono sul dispositivo
 * 
 * Version 4: Aggiunta SyncTokenEntity e HeartRateSessionEntity per
 * sincronizzazione differenziale (Changes API) e ottimizzazione HR.
 */
@Database(
    entities = [

        HeartRateSessionEntity::class,  // NEW: Ottimizzazione dati ad alta frequenza
        StepsEntity::class,
        ExerciseSessionEntity::class,
        OxygenSaturationEntity::class,
        DistanceEntity::class,
        CaloriesEntity::class,
        BasalMetabolicRateEntity::class,
        SleepSessionEntity::class,
        SleepStageEntity::class,
        SyncTokenEntity::class,          // NEW: Token per Changes API
        com.ai_health.core.data.local.entity.UserProfileEntity::class, // Added User Profile
        com.ai_health.core.data.local.entity.ChatMessageEntity::class // NEW: Chat History
    ],
    version = 7, // Incrementing version because schema changed
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun healthMetricDao(): HealthMetricDao
    abstract fun sleepDao(): SleepDao
    abstract fun syncTokenDao(): SyncTokenDao  // NEW: Per gestione token sincronizzazione
    abstract fun userDao(): com.ai_health.core.data.local.dao.UserDao
    abstract fun chatMessageDao(): com.ai_health.core.data.local.dao.ChatMessageDao
    
    // NOTA: Il metodo getDatabase() è stato rimosso.
    // La creazione del DB crittografato è gestita da DataModule via Hilt DI.
}

