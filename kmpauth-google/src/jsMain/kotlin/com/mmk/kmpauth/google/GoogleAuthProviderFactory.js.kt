package com.mmk.kmpauth.google

internal actual fun createGoogleAuthProvider(credentials: GoogleAuthCredentials): GoogleAuthProvider {
    return GoogleAuthProviderImpl(credentials)
}
