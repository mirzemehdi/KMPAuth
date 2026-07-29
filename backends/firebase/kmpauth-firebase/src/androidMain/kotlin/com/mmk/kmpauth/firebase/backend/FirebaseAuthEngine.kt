package com.mmk.kmpauth.firebase.backend

import com.mmk.kmpauth.core.auth.AuthProviderBackend

internal actual fun createFirebaseAuthEngine(): AuthProviderBackend =
    GitLiveFirebaseAuthEngine()
