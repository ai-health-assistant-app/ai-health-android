plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.ai_health.core.health"
    compileSdk = 35

    defaultConfig {
        minSdk = 28
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.health.connect.client)
    implementation(libs.kotlinx.coroutines.android)
    
    // Domain dependency is NOT strictly required here since HealthConnectManager just returns DTOs,
    // but if we share models it might be needed. The prompt says "Returns DTOs", but doesn't explicitly link to domain models yet.
    // However, clean architecture usually means:
    // core:health -> (external API wrapper) -> doesn't necessarily depend on domain unless it maps to domain entites.
    // The prompt says: "It must expose suspend functions to read Steps ... It must NOT depend on the App Database."
    // It doesn't explicitly say it depends on core:domain. 
    // BUT core:data depends on core:health AND core:domain.
    // So core:health can be independent or depend on domain if DTOs are in domain (unlikely for DTOs).
    // Let's keep it independent for now as per "Migrate Health Connect (:core:health)" instructions.
}
