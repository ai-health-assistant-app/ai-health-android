package com.ai_health.core.data.local.util

import android.content.Context
import com.ai_health.core.data.local.AppDatabase
import com.ai_health.core.data.local.entity.StepsEntity
import com.ai_health.core.data.local.entity.UserActivityEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.Instant

object RoomPrepopulator {

    fun prepopulate(context: Context, database: AppDatabase) {
        // Run on IO dispatcher
        CoroutineScope(Dispatchers.IO).launch {
             try {
                 // Check if DB is empty - just check activity logs for now as a proxy
                 // Or we rely on this being called only on onCreate which implies empty/new DB creation.
                 // Ideally we should check if table is empty.
                 
                 // However, onCreate is only called when the database file is physically created.
                 // So we can assume it's empty.
                 
                 loadActivityLogs(context, database)
                 loadSteps(context, database)
                 
             } catch (e: Exception) {
                 android.util.Log.e("RoomPrepopulator", "Error populating DB", e)
             }
        }
    }

    private suspend fun loadActivityLogs(context: Context, database: AppDatabase) {
        val fileName = "health_database-user_activity_log.csv"
        try {
            context.assets.open(fileName).use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    // Assumption: CSV header is present or not? 
                    // User said "User Activity Log... Il file contains...". 
                    // To be safe, we try to parse. If first line is header, we might catch exception or check content.
                    // For now, assume standard CSV: id,timestamp,activityType,confidence
                    
                    val entities = mutableListOf<UserActivityEntity>()
                    var line = reader.readLine()
                    // Skip header if it starts with "id" or "timestamp"
                    if (line != null && (line.startsWith("id") || line.startsWith("timestamp"))) {
                        line = reader.readLine()
                    }

                    while (line != null) {
                        // Fix generic CSV parsing: Handle both comma and semicolon
                        val tokens = if (line.contains(";")) line.split(";") else line.split(",")
                        if (tokens.size >= 3) { // id, timestamp, type, confidence
                            // The ID might be ignored if auto-generated, or preserved.
                            // UserActivityEntity: id (Long), timestamp, type, confidence
                            
                            // Let's assume the CSV matches the entity structure.
                            // If CSV export from Room: id, timestamp, activityType, confidence
                            
                            // Flexible parsing
                            try {
                                // Cleaning quotes if exported with them
                                val cleanTokens = tokens.map { it.replace("\"", "") }
                                
                                // Handling different CSV structures. 
                                // If 4 cols: id, timestamp, type, confidence
                                // If 3 cols: timestamp, type, confidence (id auto)
                                
                                // We'll try to parse timestamp from the second column if 4 cols, or first if 3.
                                // But usually Room exports include ID.
                                val timestampStr = cleanTokens[1]
                                val activityType = cleanTokens[2]
                                val confidence = cleanTokens[3].toIntOrNull() ?: 0
                                
                                val timestamp = try {
                                    Instant.parse(timestampStr) // Try ISO format first
                                } catch (e: Exception) {
                                    // Fallback to Epoch Millis
                                    Instant.ofEpochMilli(timestampStr.toLong())
                                }

                                entities.add(
                                    UserActivityEntity(
                                        timestamp = timestamp,
                                        activityType = activityType,
                                        confidence = confidence
                                    )
                                )
                            } catch (e: Exception) {
                                android.util.Log.e("RoomPrepopulator", "Failed to parse line: $line", e)
                            }
                        }
                        line = reader.readLine()
                    }
                    
                    if (entities.isNotEmpty()) {
                        database.activityLogDao().insertActivityLogs(entities)
                        android.util.Log.d("RoomPrepopulator", "Inserted ${entities.size} activity logs")
                    }
                }
            }
        } catch (e: java.io.FileNotFoundException) {
            android.util.Log.w("RoomPrepopulator", "Asset $fileName not found. Skipping.")
        }
    }

    private suspend fun loadSteps(context: Context, database: AppDatabase) {
        val fileName = "health_database-steps.csv"
        try {
            context.assets.open(fileName).use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    val entities = mutableListOf<StepsEntity>()
                    var line = reader.readLine()
                    if (line != null && (line.startsWith("id") || line.startsWith("count"))) {
                        line = reader.readLine()
                    }

                    while (line != null) {
                        val tokens = if (line.contains(";")) line.split(";") else line.split(",")
                        if (tokens.size >= 5) {
                            try {
                                val cleanTokens = tokens.map { it.replace("\"", "") }
                                // StepsEntity: id, count, startTime, endTime, source
                                // CSV assumption: id, count, startTime, endTime, source
                                
                                val id = cleanTokens[0]
                                val count = cleanTokens[1].toLongOrNull() ?: 0
                                
                                val startTimeStr = cleanTokens[2]
                                val endTimeStr = cleanTokens[3]
                                
                                val startTime = try { Instant.parse(startTimeStr) } catch(e: Exception) { Instant.ofEpochMilli(startTimeStr.toLong()) }
                                val endTime = try { Instant.parse(endTimeStr) } catch(e: Exception) { Instant.ofEpochMilli(endTimeStr.toLong()) }
                                
                                val source = cleanTokens[4] // Ensure this matches Source string expectations

                                entities.add(
                                    StepsEntity(
                                        id = id,
                                        count = count,
                                        startTime = startTime,
                                        endTime = endTime,
                                        source = source
                                    )
                                )
                            } catch (e: Exception) {
                                // Skip malformed line
                            }
                        }
                        line = reader.readLine()
                    }

                    if (entities.isNotEmpty()) {
                        database.healthMetricDao().insertSteps(entities)
                         android.util.Log.d("RoomPrepopulator", "Inserted ${entities.size} steps")
                    }
                }
            }
        } catch (e: java.io.FileNotFoundException) {
            android.util.Log.w("RoomPrepopulator", "Asset $fileName not found. Skipping.")
        }
    }
}
