package com.example.data

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

object HunterAuthManager {
    private const val TAG = "HunterAuthManager"

    fun findActivity(context: Context): Activity? {
        var currentContext = context
        while (currentContext is ContextWrapper) {
            if (currentContext is Activity) return currentContext
            currentContext = currentContext.baseContext
        }
        return null
    }

    fun getFirebaseAuth(context: Context): FirebaseAuth? {
        return try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            Log.w(TAG, "Firebase Auth not available on this device: ${e.message}")
            null
        }
    }

    suspend fun launchGoogleSignIn(context: Context): Result<Pair<String, String>> {
        val credentialManager = CredentialManager.create(context)
        val clientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
            if (resId != 0) context.getString(resId) else ""
        }

        if (clientId.isBlank()) {
            return Result.failure(IllegalStateException("Google Client ID not configured in resources."))
        }

        val activity = findActivity(context)
            ?: return Result.failure(IllegalStateException("Activity context required for Google Sign-In."))

        val googleIdOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val response = credentialManager.getCredential(context = activity, request = request)
            val credential = response.credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken
                val displayName = googleIdTokenCredential.displayName ?: "Awakened Hunter"

                val auth = getFirebaseAuth(context)
                    ?: return Result.failure(IllegalStateException("Firebase Auth service unavailable."))

                val firebaseCred = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = auth.signInWithCredential(firebaseCred).await()
                val firebaseUser = authResult.user
                    ?: return Result.failure(IllegalStateException("Failed to retrieve authenticated user."))

                Result.success(displayName to (firebaseUser.email ?: googleIdTokenCredential.id))
            } else {
                Result.failure(IllegalStateException("Unexpected credential format received."))
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w(TAG, "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
            Result.failure(e)
        } catch (e: GetCredentialException) {
            Log.w(TAG, "Credential Manager request failed: ${e.message}")
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Google Sign-In failed", e)
            Result.failure(e)
        }
    }

    suspend fun attemptAutoSignIn(context: Context): Result<Pair<String, String>> {
        val auth = getFirebaseAuth(context)
        val current = auth?.currentUser
        if (current != null) {
            return Result.success((current.displayName ?: "Awakened Hunter") to (current.email ?: current.uid))
        }

        val credentialManager = CredentialManager.create(context)
        val clientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            return Result.failure(e)
        }

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(true)
            .build()

        val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()

        return try {
            val result = credentialManager.getCredential(context, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                val res = auth!!.signInWithCredential(authCredential).await()
                val user = res.user ?: return Result.failure(IllegalStateException("User is null"))
                Result.success((user.displayName ?: "Awakened Hunter") to (user.email ?: user.uid))
            } else {
                Result.failure(IllegalStateException("Unexpected credential"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut(context: Context) {
        try {
            getFirebaseAuth(context)?.signOut()
        } catch (e: Exception) {
            Log.w(TAG, "Sign out error: ${e.message}")
        }
    }

    suspend fun clearCredentials(context: Context) {
        try {
            getFirebaseAuth(context)?.signOut()
            val credManager = CredentialManager.create(context)
            credManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            Log.w(TAG, "Clear credentials error: ${e.message}")
        }
    }
}

