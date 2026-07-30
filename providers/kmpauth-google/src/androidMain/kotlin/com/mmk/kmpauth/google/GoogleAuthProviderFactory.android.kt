package com.mmk.kmpauth.google

import androidx.credentials.CredentialManager
import com.mmk.kmpauth.core.KMPAuthInternalApi
import com.mmk.kmpauth.core.di.applicationContext

@OptIn(KMPAuthInternalApi::class)
internal actual fun createGoogleAuthProvider(credentials: GoogleAuthCredentials): GoogleAuthProvider {
    return GoogleAuthProviderImpl(
        credentials = credentials,
        credentialManager = CredentialManager.create(applicationContext),
    )
}
