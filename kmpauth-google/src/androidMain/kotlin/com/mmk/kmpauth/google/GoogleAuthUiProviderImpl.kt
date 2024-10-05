package com.mmk.kmpauth.google

import android.content.Context
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.GetCredentialUnsupportedException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException


internal class GoogleAuthUiProviderImpl(
    private val activityContext: Context,
    private val credentialManager: CredentialManager,
    private val credentials: GoogleAuthCredentials,
    private val googleLegacyAuthentication: GoogleLegacyAuthentication,
) :
    GoogleAuthUiProvider {
    override suspend fun signIn(filterByAuthorizedAccounts: Boolean): GoogleUser? {
        return try {
            getGoogleUserFromCredential(filterByAuthorizedAccounts = filterByAuthorizedAccounts)
        } catch (e: NoCredentialException) {
            if (!filterByAuthorizedAccounts) return handleCredentialException(e)
            try {
                getGoogleUserFromCredential(filterByAuthorizedAccounts = false)
            } catch (e: GetCredentialException) {
                handleCredentialException(e)
            } catch (e: NullPointerException) {
                null
            }
        } catch (e: GetCredentialException) {
            handleCredentialException(e)
        } catch (e: NullPointerException) {
            null
        }
    }

    private suspend fun handleCredentialException(e: GetCredentialException): GoogleUser? {
        println("GoogleAuthUiProvider error: ${e.message}")
        val shouldCheckLegacyAuthServices = when (e) {
            is GetCredentialProviderConfigurationException -> true
            is NoCredentialException -> true
            is GetCredentialUnsupportedException -> true
            else -> false
        }
        return if (shouldCheckLegacyAuthServices) {
            checkLegacyGoogleSignIn()
        } else {
            null
        }
    }

    private suspend fun checkLegacyGoogleSignIn(): GoogleUser? {
        println("GoogleAuthUiProvider: Checking Outdated Google Sign In...")
        return googleLegacyAuthentication.signIn()
    }

    private suspend fun getGoogleUserFromCredential(filterByAuthorizedAccounts: Boolean): GoogleUser? {
        val credential = credentialManager.getCredential(
            context = activityContext,
            request = getCredentialRequest(filterByAuthorizedAccounts)
        ).credential
        return when {
            credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL -> {
                try {
                    val googleIdTokenCredential =
                        GoogleIdTokenCredential.createFrom(credential.data)
                    GoogleUser(
                        idToken = googleIdTokenCredential.idToken,
                        accessToken = null,
                        displayName = googleIdTokenCredential.displayName ?: "",
                        profilePicUrl = googleIdTokenCredential.profilePictureUri?.toString()
                    )
                } catch (e: GoogleIdTokenParsingException) {
                    println("GoogleAuthUiProvider Received an invalid google id token response: ${e.message}")
                    null
                }
            }

            else -> null
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

    private fun getGoogleIdOption(serverClientId: String, filterByAuthorizedAccounts: Boolean): GetGoogleIdOption {
        return GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(filterByAuthorizedAccounts)
            .setAutoSelectEnabled(true)
            .setServerClientId(serverClientId)
            .build()
    }
}