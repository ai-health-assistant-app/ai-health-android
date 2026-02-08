package com.ai_health.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

/**
 * HeartRateSessionEntity - Ottimizzazione per dati ad alta frequenza.
 * 
 * PRIVACY-PROOF HIGH-FREQUENCY DATA STRATEGY:
 * - I campioni HR sono aggregati in sessioni (es. allenamento, notte)
 * - I campioni grezzi sono memorizzati come JSON compresso nel campo samplesJson
 * - Formato offset-based per risparmiare spazio: [{"o":120,"v":75},...]
 *   dove 'o' è offset in ms da startTime, 'v' è il valore BPM
 * 
 * Vantaggi:
 * - Evita esplosione delle righe DB (milioni di battiti -> poche sessioni)
 * - Mantiene fedeltà grezza per analisi HRV (variabilità cardiaca)
 * - Query veloci sulle sessioni ("allenamenti di ieri")
 * 
 * Il campo deviceType permette il filtering per dispositivi affidabili.
 */
@Entity(tableName = "heart_rate_sessions")
data class HeartRateSessionEntity(
    @PrimaryKey
    val id: String,           // Health Connect record ID (metadata.id)
    
    val source: String,       // Package name della data origin
    val deviceType: String?,  // "WATCH", "FITNESS_BAND", "PHONE", "UNKNOWN"
    val startTime: Instant,
    val endTime: Instant,
    
    /**
     * Campioni grezzi come JSON.
     * Formato: [{"o":0,"v":72},{"o":1000,"v":74},...]
     * 'o' = offsetMs rispetto a startTime (Int)
     * 'v' = valore BPM (Int)
     * 
     * Usa Kotlin Serialization (NON Gson) per performance migliori.
     */
    val samplesJson: String
)

/**
 * HeartRateSample - Singolo campione di frequenza cardiaca.
 * Usa annotazioni @SerialName per JSON compatto.
 */
@Serializable
data class HeartRateSample(
    @SerialName("o") val offsetMs: Int,  // Offset in millisecondi da session.startTime
    @SerialName("v") val bpm: Int        // Battiti per minuto
)
