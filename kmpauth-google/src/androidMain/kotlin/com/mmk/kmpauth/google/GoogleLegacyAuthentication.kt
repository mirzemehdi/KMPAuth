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
import com.google.android.gms.common.api.Scope
import com.mmk.kmpauth.core.KMPAuthInternalApi
import com.mmk.kmpauth.core.logger.currentLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

@OptIn(KMPAuthInternalApi::class)
internal class GoogleLegacyAuthentication(
    private val activityContext: Context,
    private val credentials: GoogleAuthCredentials,
    private val activityResultLauncher: ManagedActivityResultLauncher<Intent, ActivityResult>,
    private val activityResultState: ActivityResultState

) : GoogleAuthUiProvider {

    override suspend fun signIn(
        filterByAuthorizedAccounts: Boolean,
        scopes: List<String>
    ): GoogleUser? {
        val signInClient = getGoogleSignInClient(scopes = scopes).signInIntent
        activityResultState.isInProgress = true
        try {
            activityResultLauncher.launch(signInClient)
        } catch (e: ActivityNotFoundException) {
            currentLogger.log("GoogleLegacyAuth Error: $e")
            return null
        }

        withContext(Dispatchers.Default) {
            currentLogger.log("GoogleLegacyAuth: Waiting for activity result...")
            while (activityResultState.isInProgress) yield()
            currentLogger.log("GoogleLegacyAuth: Activity result is finished")
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
                    serverAuthCode = account.serverAuthCode,
                    email = account.email,
                    displayName = account.displayName ?: "",
                    profilePicUrl = account.photoUrl?.toString()
                ).also {
                    currentLogger.log("GoogleLegacy Auth is successful")
                }
            }

        } catch (e: ApiException) {
            currentLogger.log("GoogleLegacyAuth Error: $e")
            null
        }
    }

    private fun getGoogleSignInOptions(scopes: List<String>): GoogleSignInOptions {
        val builder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(credentials.serverId)
            .requestEmail()


        if (scopes != GoogleAuthUiProvider.BASIC_AUTH_SCOPE) {
            scopes.forEach { scope ->
                builder.requestScopes(Scope(scope))
            }
            builder.requestServerAuthCode(credentials.serverId)
        }


        return builder.build()
    }

    private fun getGoogleSignInClient(scopes: List<String>): GoogleSignInClient {
        return GoogleSignIn.getClient(activityContext, getGoogleSignInOptions(scopes))
    }

}