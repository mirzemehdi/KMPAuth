package com.mmk.kmpauth.google

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
        val activityResultState = remember { ActivityResultState(isInProgress = false) }
        val activityResultLauncher =
            rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                activityResultState.isInProgress = false
                activityResultState.data = result
            }
        val googleLegacyAuthentication = GoogleLegacyAuthentication(
            activityContext = activityContext,
            credentials = credentials,
            activityResultLauncher = activityResultLauncher,
            activityResultState = activityResultState
        )

        return GoogleAuthUiProviderImpl(
            activityContext = activityContext,
            credentialManager = credentialManager,
            credentials = credentials,
            googleLegacyAuthentication = googleLegacyAuthentication
        )
    }

    override suspend fun signOut() {
        credentialManager.clearCredentialState(ClearCredentialStateRequest())
    }
}