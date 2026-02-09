plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
    kotlin("plugin.serialization") version "2.1.0"
}

android {
    namespace = "com.ai_health.core.data"
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
    implementation(project(":core:domain"))
    implementation(project(":core:health"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // SQLCipher for encrypted database (Privacy-Proof)
    // Note: Using new sqlcipher-android artifact (android-database-sqlcipher is deprecated)
    implementation("net.zetetic:sqlcipher-android:4.6.1")
    implementation("androidx.sqlite:sqlite-ktx:2.4.0")

    // Security-Crypto for passphrase storage (MasterKey + EncryptedSharedPreferences)
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // WorkManager for foreground-first sync
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Kotlin Serialization for HeartRateSession samples
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // Hilt Worker (for @HiltWorker annotation)
    implementation("androidx.hilt:hilt-work:1.2.0")
    ksp("androidx.hilt:hilt-compiler:1.2.0")

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    
    // Health Connect Client (If needed for types, but usually transitive or mapped in core:health)
    // core:health exposes DTOs. core:data maps DTO -> Entity -> Domain.
    // So core:data might not need health connect client directly if core:health encapsulates it fully.
    // But let's assume core:health API is sufficient.
}
