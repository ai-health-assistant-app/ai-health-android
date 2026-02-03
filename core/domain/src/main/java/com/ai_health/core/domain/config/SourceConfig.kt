package com.ai_health.core.domain.config

/**
 * A centralized configuration object for defining data source properties.
 * This makes it easier to manage and update source classification rules without
 * modifying business logic in use cases.
 */
object SourceConfig {

    /**
     * A set of lowercase keywords that, if present in a data source's package name,
     * strongly indicate that the source is a wearable device.
     */
    val WEARABLE_KEYWORDS = setOf(
        "wearable",
        "watch",
        "garmin",
        "fitbit",
        "xiaomi"
    )
}
