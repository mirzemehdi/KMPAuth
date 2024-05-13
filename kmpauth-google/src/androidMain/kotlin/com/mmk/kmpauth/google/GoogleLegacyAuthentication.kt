package com.mmk.kmpauth.google

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.ActivityResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.util.concurrent.atomic.AtomicReference


internal class GoogleLegacyAuthentication(
    private val activityContext: Context,
    private val credentials: GoogleAuthCredentials,
    private val activityResultLauncher: ManagedActivityResultLauncher<Intent, ActivityResult>,
    private val activityResultState: ActivityResultState

) : GoogleAuthUiProvider {

    override suspend fun signIn(): GoogleUser? {
        val signInClient = getGoogleSignInClient().signInIntent
        activityResultState.isInProgress = true
        try {
            activityResultLauncher.launch(signInClient)
        }
        catch (e: ActivityNotFoundException){
            println(e.message)
            return null
        }

        withContext(Dispatchers.Default){
            while (activityResultState.isInProgress) yield()
        }
        val data: Intent? = activityResultState.data?.data
        return getGoogleUserFromIntentData(data)
    }


    private fun getGoogleUserFromIntentData(data: Intent?): GoogleUser? {
        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account: GoogleSignInAccount = task.getResult(ApiException::class.java)
            account.idToken?.let { idToken ->
                GoogleUser(
                    idToken = idToken,
                    accessToken = null,
                    displayName = account.displayName ?: "",
                    profilePicUrl = account.photoUrl?.toString()
                ).also {
                    println("GoogleLegacy Auth is successful")
                }
            }

        } catch (e: ApiException) {
            println("GoogleLegacyAuth Error: $e")
            null
        }
    }

    private fun getGoogleSignInOptions(): GoogleSignInOptions {
        return GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(credentials.serverId)
            .requestEmail()
            .build()
    }

    private fun getGoogleSignInClient(): GoogleSignInClient {
        return GoogleSignIn.getClient(activityContext, getGoogleSignInOptions())
    }

}