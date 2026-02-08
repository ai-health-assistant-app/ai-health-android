package com.ai_health.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ai_health.core.data.local.entity.SyncTokenEntity

/**
 * SyncTokenDao - DAO per la gestione dei token della Changes API.
 * 
 * PRIVACY-PROOF SYNC:
 * - getToken(): Recupera il token salvato per riprendere la sincronizzazione
 * - saveToken(): Salva il nuovo token dopo ogni sync riuscita
 * - clearToken(): Cancella il token quando scade (TOKEN_EXPIRED)
 */
@Dao
interface SyncTokenDao {
    
    @Query("SELECT token FROM sync_tokens WHERE dataType = :type")
    suspend fun getToken(type: String): String?
    
    @Query("SELECT * FROM sync_tokens WHERE dataType = :type")
    suspend fun getTokenEntity(type: String): SyncTokenEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveToken(entity: SyncTokenEntity)
    
    @Query("DELETE FROM sync_tokens WHERE dataType = :type")
    suspend fun clearToken(type: String)
    
    @Query("DELETE FROM sync_tokens")
    suspend fun clearAllTokens()
}
