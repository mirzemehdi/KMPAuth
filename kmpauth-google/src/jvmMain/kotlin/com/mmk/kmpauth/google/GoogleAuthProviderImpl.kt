package com.mmk.kmpauth.google

import androidx.compose.runtime.Composable
import com.mmk.kmpauth.core.KMPAuthInternalApi
import com.mmk.kmpauth.core.logger.currentLogger

internal class GoogleAuthProviderImpl(private val googleAuthCredentials: GoogleAuthCredentials) :
    GoogleAuthProvider {

    @Composable
    override fun getUiProvider(): GoogleAuthUiProvider {
        return GoogleAuthUiProviderImpl(credentials = googleAuthCredentials)
    }


    @OptIn(KMPAuthInternalApi::class)
    override suspend fun signOut() {
       currentLogger.log("Not implemented")
    }
}