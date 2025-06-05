package com.mmk.kmpauth.google

import android.content.Context
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
    override suspend fun signIn(
        filterByAuthorizedAccounts: Boolean,
        isAutoSelectEnabled: Boolean,
        scopes: List<String>
    ): GoogleUser? {

        val googleUser = try {
            // Temporary solution until to find out requesting additional scopes with Credential Manager.
            if (scopes != GoogleAuthUiProvider.BASIC_AUTH_SCOPE) throw GetCredentialProviderConfigurationException() //Will open Legacy Sign In

            getGoogleUserFromCredential(filterByAuthorizedAccounts = filterByAuthorizedAccounts, isAutoSelectEnabled = isAutoSelectEnabled)
        } catch (e: NoCredentialException) {
            if (!filterByAuthorizedAccounts)
                return handleCredentialException(
                    e = e,
                    filterByAuthorizedAccounts = filterByAuthorizedAccounts,
                    isAutoSelectEnabled = isAutoSelectEnabled,
                    scopes = scopes
                )
            try {
                getGoogleUserFromCredential(filterByAuthorizedAccounts = false, isAutoSelectEnabled = isAutoSelectEnabled)
            } catch (e: GetCredentialException) {
                handleCredentialException(
                    e = e,
                    filterByAuthorizedAccounts = filterByAuthorizedAccounts,
                    isAutoSelectEnabled = isAutoSelectEnabled,
                    scopes = scopes
                )
            } catch (e: NullPointerException) {
                null
            }
        } catch (e: GetCredentialException) {
            handleCredentialException(
                e = e,
                filterByAuthorizedAccounts = filterByAuthorizedAccounts,
                isAutoSelectEnabled = isAutoSelectEnabled,
                scopes = scopes
            )
        } catch (e: NullPointerException) {
            null
        }
        return googleUser
    }

    private suspend fun handleCredentialException(
        e: GetCredentialException,
        filterByAuthorizedAccounts: Boolean,
        isAutoSelectEnabled: Boolean,
        scopes: List<String>
    ): GoogleUser? {
        println("GoogleAuthUiProvider error: ${e.message}")
        val shouldCheckLegacyAuthServices = when (e) {
            is GetCredentialProviderConfigurationException -> true
            is NoCredentialException -> true
            is GetCredentialUnsupportedException -> true
            else -> false
        }
        return if (shouldCheckLegacyAuthServices) {
            checkLegacyGoogleSignIn(filterByAuthorizedAccounts, isAutoSelectEnabled, scopes)
        } else {
            null
        }
    }

    private suspend fun checkLegacyGoogleSignIn(
        filterByAuthorizedAccounts: Boolean,
        isAutoSelectEnabled: Boolean,
        scopes: List<String>
    ): GoogleUser? {
        println("GoogleAuthUiProvider: Checking Outdated Google Sign In...")
        return googleLegacyAuthentication.signIn(
            filterByAuthorizedAccounts = filterByAuthorizedAccounts,
            isAutoSelectEnabled = isAutoSelectEnabled,
            scopes = scopes
        )
    }

    private suspend fun getGoogleUserFromCredential(filterByAuthorizedAccounts: Boolean, isAutoSelectEnabled: Boolean): GoogleUser? {
        val credential = credentialManager.getCredential(
            context = activityContext,
            request = getCredentialRequest(filterByAuthorizedAccounts, isAutoSelectEnabled)
        ).credential
        return when {
            credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL -> {
                try {
                    val googleIdTokenCredential =
                        GoogleIdTokenCredential.createFrom(credential.data)
                    GoogleUser(
                        idToken = googleIdTokenCredential.idToken,
                        accessToken = null,
                        email = googleIdTokenCredential.id,
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

    private fun getCredentialRequest(
        filterByAuthorizedAccounts: Boolean,
        isAutoSelectEnabled: Boolean
    ): GetCredentialRequest {
        return GetCredentialRequest.Builder()
            .addCredentialOption(
                getGoogleIdOption(
                    serverClientId = credentials.serverId,
                    filterByAuthorizedAccounts = filterByAuthorizedAccounts,
                    isAutoSelectEnabled = isAutoSelectEnabled,
                )
            )
            .build()
    }

    private fun getGoogleIdOption(
        serverClientId: String,
        filterByAuthorizedAccounts: Boolean,
        isAutoSelectEnabled: Boolean
    ): GetGoogleIdOption {
        return GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(filterByAuthorizedAccounts)
            .setAutoSelectEnabled(isAutoSelectEnabled)
            .setServerClientId(serverClientId)
            .build()
    }
}