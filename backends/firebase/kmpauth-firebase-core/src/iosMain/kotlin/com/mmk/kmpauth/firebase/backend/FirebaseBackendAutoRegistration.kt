package com.mmk.kmpauth.firebase.backend

import com.mmk.kmpauth.core.auth.KMPAuthBackend
import kotlin.native.EagerInitialization

/**
 * Registers the Firebase backend at binary load, so having
 * `kmpauth-firebase-core` in the dependencies is enough — no explicit
 * `KMPAuth.registerBackendProvider` call needed. An app-registered backend
 * still wins when registered first (and `replace = true` always wins).
 */
@Suppress("DEPRECATION", "unused")
@OptIn(ExperimentalStdlibApi::class, com.mmk.kmpauth.core.KMPAuthInternalApi::class)
@EagerInitialization
private val firebaseBackendAutoRegistration: Unit = run {
    KMPAuthBackend.registerDefault(FirebaseAuthBackend)
}
