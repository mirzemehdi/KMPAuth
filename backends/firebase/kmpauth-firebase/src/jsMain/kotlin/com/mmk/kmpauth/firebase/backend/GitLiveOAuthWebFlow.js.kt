package com.mmk.kmpauth.firebase.backend

import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.OAuthProvider

internal actual suspend fun gitLiveOAuthWebFlowSignIn(
    oAuthProvider: OAuthProvider,
    linkAccount: Boolean,
): FirebaseUser? = throw UnsupportedOperationException(
    "Firebase OAuth web-flow sign-in is not implemented on the JS target yet."
)
