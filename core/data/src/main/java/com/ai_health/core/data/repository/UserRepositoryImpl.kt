package com.ai_health.core.data.repository

import com.ai_health.core.data.local.dao.UserDao
import com.ai_health.core.data.local.entity.UserProfileEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

import android.util.Log

private const val TAG = "UserRepositoryAuth"

class UserRepositoryImpl @Inject constructor(
    private val userDao: UserDao,
    private val auth: FirebaseAuth
) : UserRepository {

    /*
    private val auth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance().apply {
            // ...
        }
    }
    */

    override val userProfile: Flow<UserProfileEntity> = userDao.getUserProfile().map { 
        it ?: UserProfileEntity() 
    }

    override suspend fun saveUser(user: UserProfileEntity) {
        userDao.insertUser(user)
    }

    override suspend fun signInWithGoogle(idToken: String): Result<Unit> = suspendCoroutine { cont ->
        Log.d(TAG, "signInWithGoogle called with token: ${idToken.take(10)}...")
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnSuccessListener { result ->
                val user = result.user
                Log.d(TAG, "FirebaseAuth signIn success. User: ${user?.uid}, Email: ${user?.email}")
                if (user != null) {
                    // Update local user profile
                    CoroutineScope(Dispatchers.IO).launch {
                        val currentProfile = userDao.getUserProfile().first() ?: UserProfileEntity()
                        val updatedProfile = currentProfile.copy(
                            uid = user.uid,
                            name = user.displayName ?: currentProfile.name,
                            email = user.email ?: currentProfile.email,
                            photoUrl = user.photoUrl?.toString() ?: ""
                        )
                        saveUser(updatedProfile)
                        Log.d(TAG, "Local user profile updated.")
                    }
                    cont.resume(Result.success(Unit))
                } else {
                    Log.e(TAG, "FirebaseAuth success but user is null")
                    cont.resume(Result.failure(Exception("User is null")))
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "FirebaseAuth signIn failed", e)
                cont.resume(Result.failure(e))
            }
    }

    override suspend fun signOut() {
        auth.signOut()
        // Optional: Clear user profile or keep it? 
        // Requirement says "Show Sign in button". 
        // If we clear local data, we lose preferences. 
        // Let's just clear the auth fields in local DB or keep them?
        // Usually logout clears sensitive session data.
        val currentProfile = userDao.getUserProfile().first() ?: UserProfileEntity()
        val signedOutProfile = currentProfile.copy(uid = "", photoUrl = "")
        saveUser(signedOutProfile)
    }

    override suspend fun getAuthToken(): String? = suspendCoroutine { cont ->
        val user = auth.currentUser
        if (user != null) {
            user.getIdToken(true)
                .addOnSuccessListener { result ->
                    cont.resume(result.token)
                }
                .addOnFailureListener {
                    cont.resume(null)
                }
        } else {
            cont.resume(null)
        }
    }
}
