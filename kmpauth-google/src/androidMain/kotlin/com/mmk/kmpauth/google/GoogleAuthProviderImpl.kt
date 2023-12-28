package com.mmk.kmpauth.google

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager

internal class GoogleAuthProviderImpl(
    private val credentials: GoogleAuthCredentials,
    private val credentialManager: CredentialManager,
) : GoogleAuthProvider {

    @Composable
    override fun getUiProvider(): GoogleAuthUiProvider {
        val activityContext = LocalContext.current
        return GoogleAuthUiProviderImpl(
            activityContext = activityContext,
            credentialManager = credentialManager,
            credentials = credentials
        )
    }

    override suspend fun signOut() {
        credentialManager.clearCredentialState(ClearCredentialStateRequest())
    }
}