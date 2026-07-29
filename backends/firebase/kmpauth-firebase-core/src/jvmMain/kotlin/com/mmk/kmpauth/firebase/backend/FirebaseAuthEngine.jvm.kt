package com.mmk.kmpauth.firebase.backend

import com.mmk.kmpauth.core.auth.AuthProviderBackend

// GitLive's firebase-java-sdk does not implement auth on the JVM (#204);
// Desktop talks to the Firebase Auth REST API instead.
internal actual fun createFirebaseAuthEngine(): AuthProviderBackend =
    FirebaseRestAuthEngine()
