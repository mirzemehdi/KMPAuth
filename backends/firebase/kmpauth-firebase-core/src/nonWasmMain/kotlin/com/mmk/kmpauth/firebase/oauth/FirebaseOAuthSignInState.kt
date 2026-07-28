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

@OptIn(KMPAuthInternalApi::class)
@Composable
public actual fun rememberFirebaseOAuthSignInState(
    provider: String,
    requestScopes: List<String>,
    customParameters: Map<String, String>,
    linkAccount: Boolean,
    onResult: (Result<KMPAuthUser?>) -> Unit,
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
            onResult(result.map { user -> user?.let(::FirebaseKMPAuthUser) })
        },
    )
}

/**
 * Platform-specific OAuth web flow keyed on GitLive's [OAuthProvider]:
 * Android uses `startActivityForSignInWithProvider`, iOS the FirebaseAuth
 * ObjC provider flow; Desktop and JS are not implemented and report a
 * failed [Result].
 */
@Composable
internal expect fun rememberFirebaseGitLiveOAuthSignInState(
    oAuthProvider: OAuthProvider,
    linkAccount: Boolean = false,
    onResult: (Result<FirebaseUser?>) -> Unit,
): SignInState
