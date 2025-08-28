package com.mmk.kmpauth.google

import android.app.Activity
import android.content.Context
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.GetCredentialUnsupportedException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import com.mmk.kmpauth.core.KMPAuthInternalApi
import com.mmk.kmpauth.core.logger.currentLogger

@OptIn(KMPAuthInternalApi::class)
internal class GoogleAuthUiProviderImpl(
    private val activityContext: Context,
    private val credentialManager: CredentialManager,
    private val credentials: GoogleAuthCredentials,
    private val googleLegacyAuthentication: GoogleLegacyAuthentication,
    private val scopeIntentLauncher: (IntentSenderRequest) -> Unit,
    private val authResultChannel: ReceiveChannel<ActivityResult>
) :
    GoogleAuthUiProvider {
    override suspend fun signIn(
        filterByAuthorizedAccounts: Boolean,
        scopes: List<String>
    ): GoogleUser? {

        val googleUser = try {
            getGoogleUserFromCredential(
                filterByAuthorizedAccounts = filterByAuthorizedAccounts,
                scopes
            )
        } catch (e: NoCredentialException) {
            currentLogger.log("GoogleAuthUiProvider: NoCredentialException while getting credential")
            if (!filterByAuthorizedAccounts)
                return handleCredentialException(
                    e = e,
                    filterByAuthorizedAccounts = filterByAuthorizedAccounts,
                    scopes = scopes
                )
            try {
                getGoogleUserFromCredential(filterByAuthorizedAccounts = false, scopes)
            } catch (e: GetCredentialException) {
                currentLogger.log("GoogleAuthUiProvider: GetCredentialException while getting credential")
                handleCredentialException(
                    e = e,
                    filterByAuthorizedAccounts = filterByAuthorizedAccounts,
                    scopes = scopes
                )
            } catch (e: NullPointerException) {
                currentLogger.log("GoogleAuthUiProvider: NullPointerException while getting credential")
                null
            }
        } catch (e: GetCredentialException) {
            currentLogger.log("GoogleAuthUiProvider: GetCredentialException while getting credential")
            handleCredentialException(
                e = e,
                filterByAuthorizedAccounts = filterByAuthorizedAccounts,
                scopes = scopes
            )
        } catch (e: NullPointerException) {
            currentLogger.log("GoogleAuthUiProvider: NullPointerException while getting credential")
            null
        }
        return googleUser
    }


    private suspend fun handleCredentialException(
        e: GetCredentialException,
        filterByAuthorizedAccounts: Boolean,
        scopes: List<String>
    ): GoogleUser? {
        currentLogger.log("GoogleAuthUiProvider error: $e and message: ${e.message}")
        val shouldCheckLegacyAuthServices = when (e) {
            is GetCredentialProviderConfigurationException -> true
            is NoCredentialException -> true
            is GetCredentialUnsupportedException -> true
            else -> false
        }
        return if (shouldCheckLegacyAuthServices) {
            currentLogger.log("GoogleAuthUiProvider: Legacy Sign In is needed")
            checkLegacyGoogleSignIn(filterByAuthorizedAccounts, scopes)
        } else {
            currentLogger.log("GoogleAuthUiProvider: No valid credential response found")
            null
        }
    }

    @OptIn(KMPAuthInternalApi::class)
    private suspend fun checkLegacyGoogleSignIn(
        filterByAuthorizedAccounts: Boolean,
        scopes: List<String>
    ): GoogleUser? {
        currentLogger.log("GoogleAuthUiProvider: Checking Outdated Google Sign In...")
        return googleLegacyAuthentication.signIn(
            filterByAuthorizedAccounts = filterByAuthorizedAccounts,
            scopes = scopes
        )
    }

    @OptIn(KMPAuthInternalApi::class)
    private suspend fun getGoogleUserFromCredential(
        filterByAuthorizedAccounts: Boolean,
        scopes: List<String>
    ): GoogleUser? {
        val credential = credentialManager.getCredential(
            context = activityContext,
            request = getCredentialRequest(filterByAuthorizedAccounts)
        ).credential

        currentLogger.log("GoogleAuthUiProvider Received Credential: $credential")

        return when {
            credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL -> {
                try {
                    val googleIdTokenCredential =
                        GoogleIdTokenCredential.createFrom(credential.data)
                    val accessToken =
                        if (scopes != GoogleAuthUiProvider.BASIC_AUTH_SCOPE) {
                            fetchAccessTokenWithScopes(
                                scopes
                            ).accessToken
                        } else {
                            null
                        }
                    GoogleUser(
                        idToken = googleIdTokenCredential.idToken,
                        accessToken = accessToken,
                        email = googleIdTokenCredential.id,
                        displayName = googleIdTokenCredential.displayName ?: "",
                        profilePicUrl = googleIdTokenCredential.profilePictureUri?.toString()
                    )
                } catch (e: GoogleIdTokenParsingException) {
                    currentLogger.log("GoogleAuthUiProvider Received an invalid google id token response: ${e.message}")
                    null
                }
            }

            else -> {
                currentLogger.log("GoogleAuthUiProvider Received an invalid credential response: ${credential.type}")
                null
            }
        }
    }

    private suspend fun fetchAccessTokenWithScopes(scopes: List<String>): AuthorizationResult {
        val authClient = Identity.getAuthorizationClient(activityContext)
        val request = AuthorizationRequest.builder()
            .setRequestedScopes(scopes.map(::Scope))
            .build()

        return suspendCancellableCoroutine { continuation ->
            authClient.authorize(request)
                .addOnSuccessListener { r ->
                    if (r.hasResolution()) {
                        r.pendingIntent?.let { intent ->
                            scopeIntentLauncher(IntentSenderRequest.Builder(intent).build())
                            CoroutineScope(Dispatchers.Main).launch {
                                try {
                                    val result = authResultChannel.receive()
                                    val authResult = processAuthResult(activityContext, result)
                                    continuation.resume(authResult)
                                } catch (e: Exception) {
                                    if (e is CancellationException) throw e
                                    continuation.resumeWithException(e)
                                }
                            }
                        } ?: run {
                            continuation.resumeWithException(
                                IllegalStateException("Authorization has resolution but no pending intent")
                            )
                        }
                    } else {
                        continuation.resume(r)
                    }
                }
                .addOnFailureListener { e ->
                    continuation.resumeWithException(e)
                }
        }
    }

    private fun processAuthResult(ctx: Context, res: ActivityResult): AuthorizationResult {
        if (res.resultCode == Activity.RESULT_OK && res.data != null) {
            return Identity
                .getAuthorizationClient(ctx)
                .getAuthorizationResultFromIntent(res.data!!)
        } else {
            throw Exception("User cancelled authorization")
        }
    }

    private fun getCredentialRequest(filterByAuthorizedAccounts: Boolean): GetCredentialRequest {
        return GetCredentialRequest.Builder()
            .addCredentialOption(
                getGoogleIdOption(
                    serverClientId = credentials.serverId,
                    filterByAuthorizedAccounts = filterByAuthorizedAccounts
                )
            )
            .build()
    }

    private fun getGoogleIdOption(
        serverClientId: String,
        filterByAuthorizedAccounts: Boolean
    ): GetGoogleIdOption {
        return GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(filterByAuthorizedAccounts)
            .setAutoSelectEnabled(true)
            .setServerClientId(serverClientId)
            .build()
    }
}