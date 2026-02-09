package com.ai_health.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * SyncTokenEntity - Memorizza i token della Changes API di Health Connect.
 * 
 * PRIVACY-PROOF SYNC STRATEGY:
 * - Token permette sincronizzazione differenziale (solo delta)
 * - Evita letture massive (ReadRecordsRequest) privilegiando la Changes API
 * - Un token per tipo di dato supporta sincronizzazioni granulari
 * 
 * Se il token scade (TOKEN_EXPIRED da Health Connect), viene cancellato
 * e si esegue un cold-start sync degli ultimi 30 giorni.
 */
@Entity(tableName = "sync_tokens")
data class SyncTokenEntity(
    @PrimaryKey
    val dataType: String,  // "STEPS", "HEART_RATE", "SLEEP", etc. o "ALL" per token globale
    
    val token: String,     // Token opaco restituito da Health Connect
    
    val lastSyncTime: Long // Timestamp dell'ultima sincronizzazione riuscita
)
