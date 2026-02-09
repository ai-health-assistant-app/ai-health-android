package com.ai_health.core.data.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SecureKeyManager - Gestisce la passphrase del database crittografato.
 * 
 * PRIVACY-PROOF ARCHITECTURE:
 * - La passphrase è generata una sola volta per installazione (UUID random)
 * - Memorizzata in EncryptedSharedPreferences protette da MasterKey AES-256-GCM
 * - La MasterKey è custodita nel TEE (Trusted Execution Environment) via Android Keystore
 * - Nessun dato sensibile hardcoded nel codice sorgente
 * 
 * Questa implementazione segue le best practice 2025/2026 per la crittografia locale
 * come descritto nel Piano Strategico Privacy-Proof.
 */
@Singleton
class SecureKeyManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val PREFS_FILE_NAME = "secure_db_prefs"
        private const val KEY_DB_PASSPHRASE = "db_passphrase"
    }

    /**
     * MasterKey AES-256-GCM per EncryptedSharedPreferences.
     * Gestita automaticamente da Android Keystore (hardware-backed su dispositivi supportati).
     */
    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    /**
     * EncryptedSharedPreferences per memorizzare la passphrase SQLCipher.
     * - Key encryption: AES256_SIV
     * - Value encryption: AES256_GCM
     */
    private val securePrefs: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context,
            PREFS_FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /**
     * Restituisce la passphrase per SQLCipher.
     * Se non esiste, ne genera una nuova (UUID casuale).
     * 
     * @return ByteArray della passphrase per SupportFactory di SQLCipher
     */
    fun getOrCreatePassphrase(): ByteArray {
        val existing = securePrefs.getString(KEY_DB_PASSPHRASE, null)
        
        if (existing != null) {
            return existing.toByteArray(Charsets.UTF_8)
        }
        
        // Prima esecuzione: genera passphrase casuale
        val newPassphrase = UUID.randomUUID().toString()
        securePrefs.edit()
            .putString(KEY_DB_PASSPHRASE, newPassphrase)
            .apply()
        
        return newPassphrase.toByteArray(Charsets.UTF_8)
    }

    /**
     * Cancella la passphrase (usato solo per "Elimina e Disconnetti" nelle impostazioni).
     * ATTENZIONE: Questo rende il database inaccessibile permanentemente.
     */
    fun clearPassphrase() {
        securePrefs.edit()
            .remove(KEY_DB_PASSPHRASE)
            .apply()
    }
}
