package com.mmk.kmpauth.google

import androidx.compose.runtime.Composable
import cocoapods.GoogleSignIn.GIDSignIn
import kotlinx.cinterop.ExperimentalForeignApi

internal class GoogleAuthProviderImpl :
    GoogleAuthProvider {

    @Composable
    override fun getUiProvider(): GoogleAuthUiProvider = GoogleAuthUiProviderImpl()

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun signOut() {
        GIDSignIn.sharedInstance.signOut()
    }


}