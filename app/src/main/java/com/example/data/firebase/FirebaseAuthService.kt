package com.example.data.firebase

import android.util.Log
import com.example.RankifyApplication
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object FirebaseAuthService {
    private const val TAG = "FirebaseAuthService"

    private fun getAuth(): FirebaseAuth? {
        return try {
            RankifyApplication.appContext?.let { RankifyApplication.ensureFirebase(it) }
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            Log.e(TAG, "FirebaseAuth.getInstance() failed: ${e.message}", e)
            null
        }
    }

    val currentUser: FirebaseUser?
        get() = try {
            getAuth()?.currentUser
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get currentUser: ${e.message}")
            null
        }

    val authStateFlow: Flow<FirebaseUser?> = callbackFlow {
        val authInstance = getAuth()
        if (authInstance == null) {
            trySend(null)
            awaitClose {}
            return@callbackFlow
        }
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        try {
            authInstance.addAuthStateListener(listener)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to addAuthStateListener: ${e.message}")
            trySend(null)
        }
        awaitClose {
            try {
                authInstance.removeAuthStateListener(listener)
            } catch (_: Exception) {
            }
        }
    }

    suspend fun signInWithEmail(email: String, password: String): Result<FirebaseUser> {
        val authInstance = getAuth() ?: return Result.failure(
            IllegalStateException(RankifyApplication.initializationError ?: "Firebase is not initialized.")
        )
        return suspendCancellableCoroutine { continuation ->
            try {
                authInstance.signInWithEmailAndPassword(email.trim(), password)
                    .addOnSuccessListener { authResult ->
                        val user = authResult.user
                        if (user != null) {
                            continuation.resume(Result.success(user))
                        } else {
                            continuation.resume(Result.failure(Exception("Sign in failed: User is null")))
                        }
                    }
                    .addOnFailureListener { exception ->
                        continuation.resume(Result.failure(exception))
                    }
            } catch (e: Exception) {
                continuation.resume(Result.failure(e))
            }
        }
    }

    suspend fun signUpWithEmail(
        email: String,
        password: String,
        displayName: String
    ): Result<FirebaseUser> {
        val authInstance = getAuth() ?: return Result.failure(
            IllegalStateException(RankifyApplication.initializationError ?: "Firebase is not initialized.")
        )
        return suspendCancellableCoroutine { continuation ->
            try {
                authInstance.createUserWithEmailAndPassword(email.trim(), password)
                    .addOnSuccessListener { authResult ->
                        val user = authResult.user
                        if (user != null) {
                            if (displayName.isNotBlank()) {
                                val profileUpdates = UserProfileChangeRequest.Builder()
                                    .setDisplayName(displayName.trim())
                                    .build()
                                user.updateProfile(profileUpdates)
                                    .addOnCompleteListener {
                                        continuation.resume(Result.success(user))
                                    }
                            } else {
                                continuation.resume(Result.success(user))
                            }
                        } else {
                            continuation.resume(Result.failure(Exception("Account creation failed: User is null")))
                        }
                    }
                    .addOnFailureListener { exception ->
                        continuation.resume(Result.failure(exception))
                    }
            } catch (e: Exception) {
                continuation.resume(Result.failure(e))
            }
        }
    }

    suspend fun signInAnonymously(): Result<FirebaseUser> {
        val authInstance = getAuth() ?: return Result.failure(
            IllegalStateException(RankifyApplication.initializationError ?: "Firebase is not initialized.")
        )
        return suspendCancellableCoroutine { continuation ->
            try {
                authInstance.signInAnonymously()
                    .addOnSuccessListener { authResult ->
                        val user = authResult.user
                        if (user != null) {
                            continuation.resume(Result.success(user))
                        } else {
                            continuation.resume(Result.failure(Exception("Guest sign in failed: User is null")))
                        }
                    }
                    .addOnFailureListener { exception ->
                        continuation.resume(Result.failure(exception))
                    }
            } catch (e: Exception) {
                continuation.resume(Result.failure(e))
            }
        }
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> {
        val authInstance = getAuth() ?: return Result.failure(
            IllegalStateException(RankifyApplication.initializationError ?: "Firebase is not initialized.")
        )
        return suspendCancellableCoroutine { continuation ->
            try {
                authInstance.sendPasswordResetEmail(email.trim())
                    .addOnSuccessListener {
                        continuation.resume(Result.success(Unit))
                    }
                    .addOnFailureListener { exception ->
                        continuation.resume(Result.failure(exception))
                    }
            } catch (e: Exception) {
                continuation.resume(Result.failure(e))
            }
        }
    }

    fun signOut() {
        try {
            getAuth()?.signOut()
        } catch (_: Exception) {
        }
    }
}
