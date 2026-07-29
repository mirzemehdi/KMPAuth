package com.mmk.kmpauth.firebase.backend

import com.mmk.kmpauth.core.auth.AuthProviderBackend

/**
 * Platform-specific engine behind [FirebaseAuthBackend]: the native
 * Firebase SDK (GitLive bindings) on Android/iOS/JS, and the Firebase Auth
 * REST API on Desktop (JVM), where the underlying firebase-java-sdk does
 * not implement auth (#204).
 */
internal expect fun createFirebaseAuthEngine(): AuthProviderBackend
