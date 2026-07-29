package com.mmk.kmpauth.firebase.backend

import com.mmk.kmpauth.core.auth.AuthCredential
import com.mmk.kmpauth.core.auth.AuthProviderBackend
import com.mmk.kmpauth.core.auth.KMPAuthUser

/**
 * Firebase implementation of [AuthProviderBackend], KMPAuth's default
 * backend. Registered automatically the first time a Firebase sign-in state
 * is used; register a different backend at application start to override.
 *
 * Token-based credentials ([com.mmk.kmpauth.core.auth.AuthCredential.IdToken]
 * from Google or Facebook) are exchanged directly. Apple and web-flow
 * sign-in ([com.mmk.kmpauth.core.auth.AuthCredential.OAuthWebFlow]) require
 * a platform-driven browser flow and are served by the dedicated sign-in
 * states rather than this backend.
 *
 * On wasm, where the Firebase SDK has no target, every operation reports an
 * [UnsupportedOperationException] failure.
 */
public expect object FirebaseAuthBackend : AuthProviderBackend {
    // The interface's non-default members, redeclared so the commonMain
    // metadata compilation (Dokka's entry point) sees them implemented;
    // the platform actuals provide the real bodies.
    override suspend fun signIn(
        credential: AuthCredential,
        linkWithCurrentUser: Boolean,
    ): Result<KMPAuthUser>

    override suspend fun signOut()

    override fun currentUser(): KMPAuthUser?
}
