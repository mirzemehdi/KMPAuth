package com.mmk.kmpauth.firebase.email

import com.mmk.kmpauth.core.auth.KMPAuthUser
import com.mmk.kmpauth.firebase.backend.WASM_UNSUPPORTED_REASON

private fun <T> wasmUnsupported(): Result<T> =
    Result.failure(UnsupportedOperationException(WASM_UNSUPPORTED_REASON))

internal actual suspend fun firebaseEmailSignIn(
    email: String,
    password: String,
    mode: EmailAuthMode,
    linkAccount: Boolean,
): Result<KMPAuthUser?> = wasmUnsupported()

internal actual suspend fun firebaseSendPasswordResetEmail(
    email: String,
    actionCodeSettings: EmailActionCodeSettings?,
): Result<Unit> = wasmUnsupported()

internal actual suspend fun firebaseSendSignInLinkToEmail(
    email: String,
    actionCodeSettings: EmailActionCodeSettings,
): Result<Unit> = wasmUnsupported()

internal actual fun firebaseIsSignInWithEmailLink(link: String): Boolean = false

internal actual suspend fun firebaseSignInWithEmailLink(
    email: String,
    link: String,
    linkAccount: Boolean,
): Result<KMPAuthUser?> = wasmUnsupported()

internal actual suspend fun firebaseReauthenticate(
    email: String,
    password: String,
): Result<Unit> = wasmUnsupported()
