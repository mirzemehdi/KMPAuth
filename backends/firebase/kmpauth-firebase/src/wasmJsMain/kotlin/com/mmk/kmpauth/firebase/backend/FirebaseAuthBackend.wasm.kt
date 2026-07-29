package com.mmk.kmpauth.firebase.backend

import com.mmk.kmpauth.core.auth.AuthCredential
import com.mmk.kmpauth.core.auth.AuthProviderBackend
import com.mmk.kmpauth.core.auth.EmailActionCodeSettings
import com.mmk.kmpauth.core.auth.KMPAuthUser
import com.mmk.kmpauth.core.auth.PhoneVerificationUi

internal const val WASM_UNSUPPORTED_REASON: String =
    "Firebase authentication is not available on wasm: the Firebase SDK " +
        "(GitLive firebase-kotlin-sdk) has no wasm target yet."

public actual object FirebaseAuthBackend : AuthProviderBackend {

    override suspend fun signIn(
        credential: AuthCredential,
        linkWithCurrentUser: Boolean,
    ): Result<KMPAuthUser> =
        Result.failure(UnsupportedOperationException(WASM_UNSUPPORTED_REASON))

    override suspend fun reauthenticate(credential: AuthCredential): Result<Unit> =
        Result.failure(UnsupportedOperationException(WASM_UNSUPPORTED_REASON))

    override suspend fun signUp(
        email: String,
        password: String,
    ): Result<KMPAuthUser> =
        Result.failure(UnsupportedOperationException(WASM_UNSUPPORTED_REASON))

    override suspend fun signInAnonymously(): Result<KMPAuthUser> =
        Result.failure(UnsupportedOperationException(WASM_UNSUPPORTED_REASON))

    override suspend fun signInWithPhone(
        phoneNumber: String,
        verificationUi: PhoneVerificationUi,
        linkWithCurrentUser: Boolean,
    ): Result<KMPAuthUser> =
        Result.failure(UnsupportedOperationException(WASM_UNSUPPORTED_REASON))

    override suspend fun sendPasswordResetEmail(
        email: String,
        actionCodeSettings: EmailActionCodeSettings?,
    ): Result<Unit> = Result.failure(UnsupportedOperationException(WASM_UNSUPPORTED_REASON))

    override suspend fun sendSignInLinkToEmail(
        email: String,
        actionCodeSettings: EmailActionCodeSettings,
    ): Result<Unit> = Result.failure(UnsupportedOperationException(WASM_UNSUPPORTED_REASON))

    override fun isSignInWithEmailLink(link: String): Boolean = false

    override suspend fun signInWithEmailLink(
        email: String,
        link: String,
        linkAccount: Boolean,
    ): Result<KMPAuthUser> =
        Result.failure(UnsupportedOperationException(WASM_UNSUPPORTED_REASON))

    override suspend fun signOut(): Unit = Unit

    override fun currentUser(): KMPAuthUser? = null
}
