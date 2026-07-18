package com.mmk.kmpauth.firebase.apple

import com.mmk.kmpauth.apple.AppleSignInRequestScope
import androidx.compose.runtime.Composable
import com.mmk.kmpauth.core.SignInState
import com.mmk.kmpauth.firebase.oauth.rememberFirebaseOAuthSignInState
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.OAuthProvider

@Composable
public actual fun rememberFirebaseAppleSignInState(
    requestScopes: List<AppleSignInRequestScope>,
    linkAccount: Boolean,
    onResult: (Result<FirebaseUser?>) -> Unit,
): SignInState {
    val oathProviderRequestScopes = requestScopes.map {
        when (it) {
            AppleSignInRequestScope.Email -> "email"
            AppleSignInRequestScope.FullName -> "name"
        }
    }
    val oAuthProvider = OAuthProvider(provider = "apple.com", scopes = oathProviderRequestScopes)
    return rememberFirebaseOAuthSignInState(
        oAuthProvider = oAuthProvider,
        linkAccount = linkAccount,
        onResult = onResult,
    )
}
