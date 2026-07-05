package com.mmk.kmpauth.google

internal actual fun createGoogleAuthProvider(credentials: GoogleAuthCredentials): GoogleAuthProvider {
    // The iOS implementation reads its configuration from the GIDSignIn
    // shared instance; credentials are provided via Info.plist.
    return GoogleAuthProviderImpl()
}
