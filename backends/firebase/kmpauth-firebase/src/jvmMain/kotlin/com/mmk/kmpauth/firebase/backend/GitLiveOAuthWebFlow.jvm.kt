package com.mmk.kmpauth.firebase.backend

import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.OAuthProvider

// Never reached at runtime: Desktop uses the REST engine, whose
// DesktopWebAuthFlow serves OAuthWebFlow credentials.
internal actual suspend fun gitLiveOAuthWebFlowSignIn(
    oAuthProvider: OAuthProvider,
    linkAccount: Boolean,
): FirebaseUser? = throw UnsupportedOperationException(
    "The GitLive OAuth flow is not available on Desktop; the REST engine's " +
        "browser flow serves it."
)
