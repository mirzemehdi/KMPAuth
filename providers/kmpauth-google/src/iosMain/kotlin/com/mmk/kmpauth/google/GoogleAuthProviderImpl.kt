package com.mmk.kmpauth.google

import androidx.compose.runtime.Composable
import swiftPMImport.io.github.mirzemehdi.providers.kmpauth.google.GIDSignIn
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