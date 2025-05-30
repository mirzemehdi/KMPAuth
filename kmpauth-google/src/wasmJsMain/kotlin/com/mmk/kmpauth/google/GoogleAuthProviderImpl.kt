package com.mmk.kmpauth.google

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.mmk.kmpauth.core.KMPAuthInternalApi
import com.mmk.kmpauth.core.logger.currentLogger

@OptIn(KMPAuthInternalApi::class)
internal class GoogleAuthProviderImpl(
    private val credentials: GoogleAuthCredentials
) : GoogleAuthProvider {

    @Composable
    override fun getUiProvider(): GoogleAuthUiProvider = remember {
        GoogleAuthUiProviderImpl(credentials = credentials)
    }

    override suspend fun signOut() {
        currentLogger.log("User signed out from Google")
        signOutJsCode()
    }

}

private fun signOutJsCode(): Unit = js("google.accounts.id.disableAutoSelect()")

