package com.mmk.kmpauth.google

import androidx.compose.runtime.Composable

internal class GoogleAuthProviderImpl(private val googleAuthCredentials: GoogleAuthCredentials) :
    GoogleAuthProvider {

    @Composable
    override fun getUiProvider(): GoogleAuthUiProvider {
        return GoogleAuthUiProviderImpl(credentials = googleAuthCredentials)
    }


    override suspend fun signOut() {
       println("Not implemented")
    }
}