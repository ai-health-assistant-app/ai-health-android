package com.ai_health.core.data.sync

/**
 * TrustedDeviceFilter - Filtra i dati per dispositivi affidabili.
 * 
 * PRIVACY-PROOF DATA QUALITY:
 * - Priorità ai dati da dispositivi wearable (watch, fitness band, chest strap)
 * - Opzionale whitelist di package name per sorgenti specifiche
 * - Permette di escludere dati inseriti manualmente o da app inaffidabili
 * 
 * Configurabile per privilegiare device specifici (es. Oura, Garmin, Pixel Watch)
 * rispetto ai dati inseriti manualmente o da app meno affidabili.
 */
object TrustedDeviceFilter {
    
    /**
     * Tipi di dispositivo considerati affidabili per dati fisiologici.
     * WATCH e FITNESS_BAND hanno sensori ottici; CHEST_STRAP ha sensori ECG.
     */
    private val TRUSTED_DEVICE_TYPES = setOf(
        "WATCH",
        "FITNESS_BAND",
        "CHEST_STRAP",
        "RING"          // Smart ring come Oura
    )
    
    /**
     * Package name di app wearable note e affidabili.
     * Questa lista può essere estesa/configurata dall'utente.
     */
    private val TRUSTED_PACKAGES = setOf(
        // Xiaomi / Mi Fit
        "com.xiaomi.wearable",
        "com.mi.health",
        "com.xiaomi.hm.health",
        
        // Garmin
        "com.garmin.android.apps.connectmobile",
        
        // Google
        "com.google.android.apps.fitness",
        "com.google.android.apps.healthdata",
        
        // Samsung
        "com.samsung.android.wear.shealth",
        "com.samsung.shealth",
        
        // Fitbit
        "com.fitbit.FitbitMobile",
        
        // Polar
        "fi.polar.polarflow",
        
        // Oura
        "com.ouraring.oura",
        
        // Whoop
        "com.whoop.android",
        
        // Withings
        "com.withings.wiscale2"
    )
    
    /**
     * Filtra una lista di record mantenendo solo quelli da dispositivi affidabili.
     * 
     * @param records Lista di record da filtrare
     * @param getDeviceType Lambda per estrarre il deviceType dal record
     * @param getPackage Lambda per estrarre il package name dal record
     * @return Lista filtrata con solo record da fonti affidabili
     */
    fun <T> filterTrusted(
        records: List<T>,
        getDeviceType: (T) -> String?,
        getPackage: (T) -> String
    ): List<T> {
        return records.filter { record ->
            val deviceType = getDeviceType(record)
            val packageName = getPackage(record)
            
            // Accetta se il device type è affidabile O se il package è nella whitelist
            deviceType in TRUSTED_DEVICE_TYPES || packageName in TRUSTED_PACKAGES
        }
    }
    
    /**
     * Verifica se un singolo record proviene da una fonte affidabile.
     */
    fun isTrusted(deviceType: String?, packageName: String): Boolean {
        return deviceType in TRUSTED_DEVICE_TYPES || packageName in TRUSTED_PACKAGES
    }
    
    /**
     * Prioritizza i record per qualità del dato.
     * Ordina: CHEST_STRAP > WATCH/FITNESS_BAND > RING > altri.
     */
    fun <T> sortByQuality(
        records: List<T>,
        getDeviceType: (T) -> String?
    ): List<T> {
        val priorityMap = mapOf(
            "CHEST_STRAP" to 0,   // ECG, massima precisione
            "WATCH" to 1,
            "FITNESS_BAND" to 1,
            "RING" to 2,
            "PHONE" to 3,
            "UNKNOWN" to 4
        )
        
        return records.sortedBy { record ->
            val deviceType = getDeviceType(record) ?: "UNKNOWN"
            priorityMap[deviceType] ?: 5
        }
    }
}
