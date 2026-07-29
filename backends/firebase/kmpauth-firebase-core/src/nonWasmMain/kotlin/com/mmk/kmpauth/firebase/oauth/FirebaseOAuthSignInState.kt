package com.mmk.kmpauth.firebase.oauth

import androidx.compose.runtime.Composable
import com.mmk.kmpauth.core.KMPAuthInternalApi
import com.mmk.kmpauth.core.SignInState
import com.mmk.kmpauth.core.auth.KMPAuthBackend
import com.mmk.kmpauth.core.auth.KMPAuthUser
import com.mmk.kmpauth.firebase.backend.FirebaseAuthBackend
import com.mmk.kmpauth.firebase.backend.FirebaseKMPAuthUser
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.OAuthProvider

/**
 * Shared android/ios/js implementation of `rememberOAuthState`, bridging
 * to the GitLive-typed platform web flows. Desktop (JVM) has its own
 * actual that routes through the backend's browser flow instead.
 */
@OptIn(KMPAuthInternalApi::class)
@Composable
internal fun rememberOAuthStateViaGitLive(
    provider: String,
    requestScopes: List<String>,
    customParameters: Map<String, String>,
    linkAccount: Boolean,
    onResult: (Result<KMPAuthUser>) -> Unit,
): SignInState {
    // Lazy default registration: no-op when the app already registered
    // a backend at startup (first registration wins).
    KMPAuthBackend.register(FirebaseAuthBackend)
    val oAuthProvider = OAuthProvider(
        provider = provider,
        scopes = requestScopes,
        customParameters = customParameters,
    )
    return rememberFirebaseGitLiveOAuthSignInState(
        oAuthProvider = oAuthProvider,
        linkAccount = linkAccount,
        onResult = { result ->
            onResult(
                result.fold(
                    onSuccess = { user ->
                        user?.let { Result.success<KMPAuthUser>(FirebaseKMPAuthUser(it)) }
                            ?: Result.failure(IllegalStateException("Firebase Null user"))
                    },
                    onFailure = { Result.failure(it) },
                )
            )
        },
    )
}

/**
 * Platform-specific OAuth web flow keyed on GitLive's [OAuthProvider]:
 * Android uses `startActivityForSignInWithProvider`, iOS the FirebaseAuth
 * ObjC provider flow; JS is not implemented and reports a failed [Result].
 * (Desktop's `rememberOAuthState` bypasses this entirely.)
 */
@Composable
internal expect fun rememberFirebaseGitLiveOAuthSignInState(
    oAuthProvider: OAuthProvider,
    linkAccount: Boolean = false,
    onResult: (Result<FirebaseUser?>) -> Unit,
): SignInState
