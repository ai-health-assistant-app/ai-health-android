package com.ai_health.core.data.di

import android.util.Log
import com.ai_health.core.data.remote.ChatApi
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.firestoreSettings
import com.google.firebase.ktx.Firebase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import com.google.android.gms.tasks.Tasks

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    // === Cloudflare Tunnels Configuration ===
    
    // 1. Chat Backend (Retrofit)
    //private const val CHAT_BASE_URL = "https://example.com/"
    private const val CHAT_BASE_URL = "http://10.0.2.2:8001"
    
    // 2. Firebase Emulators (Firebase SDK)
    // INCOLLA QUI I TUOI URL DI CLOUDFLARE (Senza https:// e senza slash finale)
    //private const val AUTH_HOST = "example.com"      // Tunnel porta 9099

    private const val AUTH_HOST = "10.0.2.2"
    //private const val FIRESTORE_HOST = "example.com" // Tunnel porta 8080

    private const val FIRESTORE_HOST = "10.0.2.2"

    // --- Retrofit & Chat API ---

    @Provides
    @Singleton
    fun provideOkHttpClient(auth: FirebaseAuth): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val authInterceptor = Interceptor { chain ->
            val requestBuilder = chain.request().newBuilder()
            
            val user = auth.currentUser
            if (user != null) {
                try {
                    val tokenResult = Tasks.await(user.getIdToken(false))
                    tokenResult.token?.let { token ->
                        requestBuilder.addHeader("Authorization", "Bearer $token")
                    }
                } catch (e: Exception) {
                    Log.e("NetworkModule", "Error fetching Firebase token", e)
                    throw java.io.IOException("Failed to fetch Firebase token", e)
                }
            }
            
            chain.proceed(requestBuilder.build())
        }

        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
        .baseUrl(CHAT_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    }

    @Provides
    @Singleton
    fun provideChatApi(retrofit: Retrofit): ChatApi {
        return retrofit.create(ChatApi::class.java)
    }

    // --- Firebase Emulators ---

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        val auth = Firebase.auth
        try {
            // Usa la porta 80 per HTTP (FirebaseAuth.useEmulator usa cleartext)
            // Cloudflare Tunnel accetta HTTP sulla porta 80
            auth.useEmulator(AUTH_HOST, 9099)
            Log.d("FirebaseModule", "Auth Emulator collegato a: $AUTH_HOST:9099")
        } catch (e: Exception) {
            Log.w("FirebaseModule", "Auth Emulator forse già configurato", e)
        }
        return auth
    }

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        val firestore = Firebase.firestore
        try {
            // Usa la porta 80 per HTTP
            firestore.useEmulator(FIRESTORE_HOST, 8080)
            
            // Disabilita la persistenza per evitare conflitti con la cache locale durante i test
            val settings = firestoreSettings {
                isPersistenceEnabled = false
            }
            firestore.firestoreSettings = settings
            
            Log.d("FirebaseModule", "Firestore Emulator collegato a: $FIRESTORE_HOST:8080")
        } catch (e: Exception) {
            Log.w("FirebaseModule", "Firestore Emulator forse già configurato", e)
        }
        return firestore
    }
}