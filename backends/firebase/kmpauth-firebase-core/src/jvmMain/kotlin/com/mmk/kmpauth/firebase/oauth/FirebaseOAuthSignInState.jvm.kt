package com.mmk.kmpauth.firebase.oauth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import com.mmk.kmpauth.core.KMPAuthInternalApi
import com.mmk.kmpauth.core.LaunchingSignInState
import com.mmk.kmpauth.core.SignInState
import com.mmk.kmpauth.core.auth.AuthCredential
import com.mmk.kmpauth.core.auth.KMPAuthBackend
import com.mmk.kmpauth.core.auth.KMPAuthUser
import com.mmk.kmpauth.firebase.backend.FirebaseAuthBackend

@OptIn(KMPAuthInternalApi::class)
@Composable
public actual fun rememberOAuthState(
    provider: String,
    requestScopes: List<String>,
    customParameters: Map<String, String>,
    linkAccount: Boolean,
    onResult: (Result<KMPAuthUser>) -> Unit,
): SignInState {
    val scope = rememberCoroutineScope()
    val currentProvider by rememberUpdatedState(provider)
    val currentScopes by rememberUpdatedState(requestScopes)
    val currentCustomParameters by rememberUpdatedState(customParameters)
    val currentLinkAccount by rememberUpdatedState(linkAccount)
    val currentOnResult by rememberUpdatedState(onResult)

    return remember {
        // Lazy default registration: no-op when the app already registered
        // a backend at startup (first registration wins).
        KMPAuthBackend.register(FirebaseAuthBackend)
        LaunchingSignInState(scope) {
            // On Desktop the backend runs the flow in the system browser via
            // Firebase's hosted auth handler (DesktopWebAuthFlow).
            currentOnResult(
                KMPAuthBackend.signIn(
                    credential = AuthCredential.OAuthWebFlow(
                        providerId = currentProvider,
                        scopes = currentScopes,
                        customParameters = currentCustomParameters,
                    ),
                    linkWithCurrentUser = currentLinkAccount,
                )
            )
        }
    }
}

// Satisfies the internal GitLive-typed expect for the JVM target; Desktop's
// public rememberOAuthState above never routes through it.
@OptIn(KMPAuthInternalApi::class)
@Composable
internal actual fun rememberFirebaseGitLiveOAuthSignInState(
    oAuthProvider: dev.gitlive.firebase.auth.OAuthProvider,
    linkAccount: Boolean,
    onResult: (Result<dev.gitlive.firebase.auth.FirebaseUser?>) -> Unit,
): SignInState {
    val currentOnResult by rememberUpdatedState(onResult)
    return remember {
        com.mmk.kmpauth.core.UnsupportedSignInState(
            reason = "The GitLive OAuth flow is not available on Desktop; use rememberOAuthState.",
            onFailure = { currentOnResult(Result.failure(it)) },
        )
    }
}
