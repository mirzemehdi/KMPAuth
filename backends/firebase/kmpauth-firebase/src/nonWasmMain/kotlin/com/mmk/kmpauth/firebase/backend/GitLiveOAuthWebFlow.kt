package com.mmk.kmpauth.firebase.backend

import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.OAuthProvider

/**
 * Runs Firebase's OAuth web flow for [oAuthProvider] and returns the
 * signed-in (or linked) user: `startActivityForSignInWithProvider` on
 * Android (Activity from core's `AndroidActivityTracker`), the FirebaseAuth
 * ObjC provider flow on iOS. Throws [UnsupportedOperationException] on
 * JS (not implemented yet) and on the JVM (the REST engine's
 * `DesktopWebAuthFlow` serves Desktop instead).
 */
internal expect suspend fun gitLiveOAuthWebFlowSignIn(
    oAuthProvider: OAuthProvider,
    linkAccount: Boolean,
): FirebaseUser?
