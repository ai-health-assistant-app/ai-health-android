package com.ai_health.core.data.repository

import com.ai_health.core.data.local.dao.UserDao
import com.ai_health.core.data.local.entity.UserProfileEntity
import com.ai_health.core.domain.model.User
import com.ai_health.core.domain.repository.UserRepository
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
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

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

    override val userProfile: Flow<User> = userDao.getUserProfile().map { 
        val entity = it ?: UserProfileEntity() 
        User(
            id = entity.id,
            name = entity.name,
            surname = entity.surname,
            email = entity.email,
            photoUrl = entity.photoUrl,
            uid = entity.uid,
            birthDate = entity.birthDate,
            gender = entity.gender,
            weight = entity.weight,
            height = entity.height
        )
    }

    override suspend fun saveUser(user: User) {
        val entity = UserProfileEntity(
            id = user.id,
            name = user.name,
            surname = user.surname,
            email = user.email,
            photoUrl = user.photoUrl,
            uid = user.uid,
            birthDate = user.birthDate,
            gender = user.gender,
            weight = user.weight,
            height = user.height
        )
        userDao.insertUser(entity)
    }

    override suspend fun signInWithGoogle(idToken: String): Result<Unit> = suspendCancellableCoroutine { cont ->
        Log.d(TAG, "signInWithGoogle called with token: ${idToken.take(10)}...")
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnSuccessListener { result ->
                val user = result.user
                Log.d(TAG, "FirebaseAuth signIn success. User: ${user?.uid}, Email: ${user?.email}")
                if (user != null) {
                    // Update local user profile
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val currentProfile = userDao.getUserProfile().first() ?: UserProfileEntity()
                            val updatedProfile = User(
                                id = currentProfile.id,
                                name = user.displayName ?: currentProfile.name,
                                surname = currentProfile.surname,
                                email = user.email ?: currentProfile.email,
                                photoUrl = user.photoUrl?.toString() ?: currentProfile.photoUrl,
                                uid = user.uid,
                                birthDate = currentProfile.birthDate,
                                gender = currentProfile.gender,
                                weight = currentProfile.weight,
                                height = currentProfile.height
                            )
                            saveUser(updatedProfile)
                            Log.d(TAG, "Local user profile updated.")
                            if (cont.isActive) cont.resume(Result.success(Unit))
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to update local user profile", e)
                            if (cont.isActive) cont.resume(Result.failure(e))
                        }
                    }
                } else {
                    Log.e(TAG, "FirebaseAuth success but user is null")
                    if (cont.isActive) cont.resume(Result.failure(Exception("User is null")))
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "FirebaseAuth signIn failed", e)
                if (cont.isActive) cont.resume(Result.failure(e))
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
        val signedOutProfile = User(
            id = currentProfile.id,
            name = currentProfile.name,
            surname = currentProfile.surname,
            email = currentProfile.email,
            photoUrl = "",
            uid = "",
            birthDate = currentProfile.birthDate,
            gender = currentProfile.gender,
            weight = currentProfile.weight,
            height = currentProfile.height
        )
        saveUser(signedOutProfile)
    }

    override suspend fun getAuthToken(): String? = suspendCancellableCoroutine { cont ->
        val user = auth.currentUser
        if (user != null) {
            user.getIdToken(true)
                .addOnSuccessListener { result ->
                    if (cont.isActive) cont.resume(result.token)
                }
                .addOnFailureListener {
                    if (cont.isActive) cont.resume(null)
                }
        } else {
            if (cont.isActive) cont.resume(null)
        }
    }
}
