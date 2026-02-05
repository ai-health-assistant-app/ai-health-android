package com.ai_health.core.data.repository

import android.util.Log
import com.ai_health.core.data.local.AppDatabase
import com.ai_health.core.data.mapper.HealthMappers
import com.ai_health.core.data.mapper.HealthMappers.toDomain
import com.ai_health.core.data.mapper.HealthMappers.toEntity
import com.ai_health.core.domain.model.BasalMetabolicRateRec
import com.ai_health.core.domain.model.CaloriesRec
import com.ai_health.core.domain.model.DistanceRec
import com.ai_health.core.domain.model.ExerciseSessionRec
import com.ai_health.core.domain.model.HeartRateRec
import com.ai_health.core.domain.model.OxygenSaturationRec
import com.ai_health.core.domain.model.SleepSessionRec
import com.ai_health.core.domain.model.StepsRec
import com.ai_health.core.domain.repository.HealthRepository
import com.ai_health.core.health.HealthConnectManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.firstOrNull
import androidx.datastore.preferences.core.edit
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject

class HealthRepositoryImpl @Inject constructor(
    private val healthConnectManager: HealthConnectManager,
    private val db: AppDatabase,
    private val healthConnectNormalizer: com.ai_health.core.data.normalization.HealthConnectNormalizer,
    private val dataStore: androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>
) : HealthRepository {

    private val healthDao = db.healthMetricDao()
    private val sleepDao = db.sleepDao()
    private val TAG = "HealthRepo"
    
    private val TOKEN_KEY = androidx.datastore.preferences.core.stringPreferencesKey("health_connect_changes_token")

    override suspend fun syncHealthData() {
        if (!healthConnectManager.hasAllPermissions()) return

        val token = getNameToken()
        if (token == null) {
            Log.d(TAG, "No token found. Performing Cold Start.")
            performColdStart()
        } else {
            Log.d(TAG, "Token found. Performing Incremental Sync.")
            syncChanges(token)
        }
    }

    private suspend fun getNameToken(): String? {
        val prefs = dataStore.data.firstOrNull() ?: return null
        return prefs[TOKEN_KEY]
    }

    private suspend fun saveToken(token: String) {
        dataStore.edit { prefs ->
            prefs[TOKEN_KEY] = token
        }
    }

    private suspend fun performColdStart() {
        // Fetch last 30 days
        val end = Instant.now()
        val start = java.time.LocalDate.now().minusDays(30)
            .atStartOfDay(ZoneId.systemDefault()).toInstant()

        fetchAndStoreRaw(start, end)

        // Get and save new token
        val newToken = healthConnectManager.getChangesToken()
        if (newToken != null) {
            saveToken(newToken)
        }
    }

    private suspend fun syncChanges(token: String) {
        try {
            val changes = healthConnectManager.getChanges(token) ?: return
            
            // Process Upserts
            changes.changes.filterIsInstance<androidx.health.connect.client.changes.UpsertionChange>().forEach { change ->
                processUpsertion(change.record)
            }
            
            // Process Deletes
            changes.changes.filterIsInstance<androidx.health.connect.client.changes.DeletionChange>().forEach { change ->
                // Delete from all tables by ID
                healthDao.deleteRecordById(change.recordId)
                sleepDao.deleteSessionById(change.recordId) // Assuming separate delete logic for sleep
            }

            // Save next token
            if (changes.hasMore) {
                syncChanges(changes.nextChangesToken)
            } else {
                saveToken(changes.nextChangesToken)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error during syncChanges", e)
            // Check for ChangesTokenOutdated or similar (generic Exception check for now or specific if possible)
            // Health Connect throws unchecked exceptions often, but doc says checking message or type is good.
            // Simplified: If error assumes token invalidation -> Soft Reset.
            // But be careful not to loop.
            // Assuming "outdated" or "invalid" token.
            Log.w(TAG, "Token might be expired or invalid. Resetting.")
            dataStore.edit { it.remove(TOKEN_KEY) }
            performColdStart()
        }
    }

    private suspend fun processUpsertion(record: androidx.health.connect.client.records.Record) {
        when (record) {
            is androidx.health.connect.client.records.StepsRecord -> {
                healthDao.insertSteps(listOf(
                    com.ai_health.core.data.local.entity.StepsEntity(
                        id = record.metadata.id,
                        count = record.count,
                        startTime = record.startTime,
                        endTime = record.endTime,
                        source = record.metadata.dataOrigin.packageName
                    )
                ))
            }
            is androidx.health.connect.client.records.HeartRateRecord -> {
                val avgBpm = if (record.samples.isNotEmpty()) record.samples.map { it.beatsPerMinute }.average() else 0.0
                healthDao.insertHeartRates(listOf(
                    com.ai_health.core.data.local.entity.HeartRateEntity(
                        id = record.metadata.id,
                        beatsPerMinute = avgBpm.toLong(),
                        time = record.startTime,
                        source = record.metadata.dataOrigin.packageName
                    )
                ))
            }
            is androidx.health.connect.client.records.SleepSessionRecord -> {
                 val sessionEntity = com.ai_health.core.data.local.entity.SleepSessionEntity(
                     id = record.metadata.id,
                     title = record.title,
                     notes = record.notes,
                     startTime = record.startTime,
                     endTime = record.endTime,
                     source = record.metadata.dataOrigin.packageName
                 )
                 val stages = record.stages.map { stage ->
                     com.ai_health.core.data.local.entity.SleepStageEntity(
                         id = HealthMappers.generateId("SLEEP_STAGE", stage.startTime), // Helper ID
                         source = sessionEntity.source,
                         sleepSessionId = sessionEntity.id,
                         stage = stage.stage,
                         startTime = stage.startTime,
                         endTime = stage.endTime
                     )
                 }
                 sleepDao.insertSleepWithStages(sessionEntity, stages)
            }
            is androidx.health.connect.client.records.ActiveCaloriesBurnedRecord -> {
                healthDao.insertCalories(listOf(
                    com.ai_health.core.data.local.entity.CaloriesEntity(
                        id = record.metadata.id,
                        energyKilocalories = record.energy.inKilocalories,
                        startTime = record.startTime,
                        endTime = record.endTime,
                        source = record.metadata.dataOrigin.packageName
                    )
                ))
            }
            is androidx.health.connect.client.records.DistanceRecord -> {
                healthDao.insertDistances(listOf(
                    com.ai_health.core.data.local.entity.DistanceEntity(
                        id = record.metadata.id,
                        distanceMeters = record.distance.inMeters,
                        startTime = record.startTime,
                        endTime = record.endTime,
                        source = record.metadata.dataOrigin.packageName
                    )
                ))
            }
            is androidx.health.connect.client.records.OxygenSaturationRecord -> {
                healthDao.insertOxygen(listOf(
                     com.ai_health.core.data.local.entity.OxygenSaturationEntity(
                         id = record.metadata.id,
                         percentage = record.percentage.value,
                         time = record.time,
                         source = record.metadata.dataOrigin.packageName
                     )
                ))
            }
            is androidx.health.connect.client.records.ExerciseSessionRecord -> {
                healthDao.insertExercises(listOf(
                     com.ai_health.core.data.local.entity.ExerciseSessionEntity(
                         id = record.metadata.id,
                         exerciseType = "Exercise_${record.exerciseType}", // Manual mapping needed possibly?
                         title = record.title,
                         notes = record.notes,
                         startTime = record.startTime,
                         endTime = record.endTime,
                         source = record.metadata.dataOrigin.packageName
                     )
                ))
            }
            is androidx.health.connect.client.records.BasalMetabolicRateRecord -> {
                healthDao.insertBmr(listOf(
                     com.ai_health.core.data.local.entity.BasalMetabolicRateEntity(
                         id = record.metadata.id,
                         energyKilocaloriesPerDay = record.basalMetabolicRate.inKilocaloriesPerDay,
                         time = record.time,
                         source = record.metadata.dataOrigin.packageName
                     )
                ))
            }
        }
    }

    private suspend fun fetchAndStoreRaw(start: Instant, end: Instant) {
        // Reuse Manager's fetch methods but mapped to Entity
        // Note: Manager returns DTOs (RawSteps etc). 
        // We can reuse the logic from the old syncHealthData but parameterized.
        
        // A. STEPS
        val steps = healthConnectManager.fetchSteps(start, end)
        healthDao.insertSteps(steps.map { 
            com.ai_health.core.data.local.entity.StepsEntity(it.id, it.count, Instant.ofEpochMilli(it.startTime), Instant.ofEpochMilli(it.endTime), it.sourcePackage) 
        })

        // B. SLEEP
        val sleep = healthConnectManager.fetchSleep(start, end)
        sleep.forEach { s ->
            val session = com.ai_health.core.data.local.entity.SleepSessionEntity(s.id, s.sourcePackage, null, null, Instant.ofEpochMilli(s.startTime), Instant.ofEpochMilli(s.endTime))
            val stages = s.stages.map { st ->
                 com.ai_health.core.data.local.entity.SleepStageEntity(HealthMappers.generateId("SLEEP_STAGE", Instant.ofEpochMilli(st.startTime)), s.sourcePackage, s.id, st.stage, Instant.ofEpochMilli(st.startTime), Instant.ofEpochMilli(st.endTime))
            }
            sleepDao.insertSleepWithStages(session, stages)
        }

        // C. HEART RATE
        val heart = healthConnectManager.fetchHeartRate(start, end)
        healthDao.insertHeartRates(heart.map {
            com.ai_health.core.data.local.entity.HeartRateEntity(it.id, it.sourcePackage, it.bpm.toLong(), Instant.ofEpochMilli(it.startTime))
        })
        
        // ... (Other types following same pattern) ...
        // For brevity in this replacing chunk, I will assume similar calls for others or add them.
        // But to be safe I should implement all.
        
        val cals = healthConnectManager.fetchCalories(start, end)
        healthDao.insertCalories(cals.map {
             com.ai_health.core.data.local.entity.CaloriesEntity(it.id, it.sourcePackage, it.kilocalories, Instant.ofEpochMilli(it.startTime), Instant.ofEpochMilli(it.endTime))
        })

        val dist = healthConnectManager.fetchDistance(start, end)
        healthDao.insertDistances(dist.map {
             com.ai_health.core.data.local.entity.DistanceEntity(it.id, it.sourcePackage, it.distanceMeters, Instant.ofEpochMilli(it.startTime), Instant.ofEpochMilli(it.endTime))
        })
        
        val oxy = healthConnectManager.fetchOxygenSaturation(start, end)
        healthDao.insertOxygen(oxy.map {
             com.ai_health.core.data.local.entity.OxygenSaturationEntity(it.id, it.sourcePackage, it.percentage, Instant.ofEpochMilli(it.startTime))
        })
        
        val exer = healthConnectManager.fetchExercise(start, end)
        healthDao.insertExercises(exer.map {
             com.ai_health.core.data.local.entity.ExerciseSessionEntity(it.id, it.sourcePackage, it.type, it.title, it.notes, Instant.ofEpochMilli(it.startTime), Instant.ofEpochMilli(it.endTime))
        })
        
        val bmr = healthConnectManager.fetchBMR(start, end)
        healthDao.insertBmr(bmr.map {
             com.ai_health.core.data.local.entity.BasalMetabolicRateEntity(it.id, it.sourcePackage, it.kcalPerDay, Instant.ofEpochMilli(it.startTime))
        })
    }

    // --- ACCESSORS WITH LAZY LOAD ---

    override fun getStepsHistory(startTime: Instant): Flow<List<StepsRec>> {
        return healthDao.getSteps(startTime)
            .onStart { checkAndFetchDay(startTime) }
            .map { list -> list.map { it.toDomain() } }
    }

    override fun getSleepHistory(startTime: Instant): Flow<List<SleepSessionRec>> {
         return sleepDao.getSleepSessions(startTime)
            .onStart { checkAndFetchDay(startTime) }
            .map { list -> list.map { it.toDomain() } }
    }

    override fun getHeartRateHistory(startTime: Instant): Flow<List<HeartRateRec>> {
         return healthDao.getHeartRates(startTime)
            .onStart { checkAndFetchDay(startTime) }
            .map { list -> list.map { it.toDomain() } }
    }
    
    override fun getCaloriesHistory(startTime: Instant): Flow<List<CaloriesRec>> {
         return healthDao.getCalories(startTime)
            .onStart { checkAndFetchDay(startTime) }
            .map { list -> list.map { it.toDomain() } }
    }
     
    override fun getDistanceHistory(startTime: Instant): Flow<List<DistanceRec>> {
         return healthDao.getDistances(startTime)
            .onStart { checkAndFetchDay(startTime) }
            .map { list -> list.map { it.toDomain() } }
    }
     
    override fun getOxygenHistory(startTime: Instant): Flow<List<OxygenSaturationRec>> {
         return healthDao.getOxygenSaturation(startTime)
            .onStart { checkAndFetchDay(startTime) }
            .map { list -> list.map { it.toDomain() } }
    }
    
    override fun getExerciseHistory(startTime: Instant): Flow<List<ExerciseSessionRec>> {
         return healthDao.getExercises(startTime)
            .onStart { checkAndFetchDay(startTime) }
            .map { list -> list.map { it.toDomain() } }
    }
    
    override fun getBasalMetabolicRateHistory(startTime: Instant): Flow<List<BasalMetabolicRateRec>> {
         return healthDao.getBmr(startTime)
            .onStart { checkAndFetchDay(startTime) }
            .map { list -> list.map { it.toDomain() } }
    }

    private suspend fun checkAndFetchDay(startTime: Instant) {
        // Logic: Check if we have data for this day. simpler: check if we have data for the last 24h from startTime??
        // The accessor gives a `startTime` (history start). User usually asks for a day.
        // If DB has 0 items after `startTime`, maybe we should fetch.
        // But `startTime` could be 1 year ago. Fetching 1 year is bad.
        // "Quando l'UI richiede i dati di un giorno specifico"
        // The standard usage is likely: getStepsHistory( startOfDay ).
        // So we fetch that day (start -> start + 24h).
        // Since we don't have an "end time" in the accessor, we assume we fetch "from startTime to Now (or end of day)".
        // BUT, better to fetch just that day to be safe.
        // Actually, if we fetch 30 days on cold start, recent history is there.
        // If user scrolls back further, we need to fetch.
        // Let's assume we fetch `startTime` to `startTime + 24h`.
        
        val count = healthDao.getStepsCountForIntervall(startTime, startTime.plus(1, ChronoUnit.DAYS))
        if (count == 0) {
             val end = startTime.plus(1, ChronoUnit.DAYS).coerceAtMost(Instant.now())
             Log.d(TAG, "Lazy loading for $startTime")
             fetchAndStoreRaw(startTime, end)
        }
    }
}
