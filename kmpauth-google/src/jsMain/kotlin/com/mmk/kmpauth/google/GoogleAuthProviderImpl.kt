package com.mmk.kmpauth.google

import androidx.compose.runtime.Composable

internal class GoogleAuthProviderImpl : GoogleAuthProvider {

    @Composable
    override fun getUiProvider(): GoogleAuthUiProvider {
        TODO("Not yet implemented")
    }

    override suspend fun signOut() {
        TODO("Not yet implemented")
    }
}