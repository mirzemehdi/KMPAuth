package com.mmk.kmpauth.firebase.apple

import androidx.compose.runtime.Composable
import com.mmk.kmpauth.apple.AppleSignInRequestScope
import com.mmk.kmpauth.core.SignInState
import com.mmk.kmpauth.core.auth.KMPAuthUser
import com.mmk.kmpauth.firebase.oauth.rememberOAuthState

@Composable
public actual fun rememberAppleAuthState(
    requestScopes: List<AppleSignInRequestScope>,
    linkAccount: Boolean,
    onResult: (Result<KMPAuthUser>) -> Unit,
): SignInState {
    // Same shape as Android: Apple runs as a Firebase OAuth web flow. On
    // Desktop the browser opens Firebase's hosted auth handler, whose https
    // redirect is what Apple requires (direct localhost redirects are not
    // allowed by Apple).
    val oathProviderRequestScopes = requestScopes.map {
        when (it) {
            AppleSignInRequestScope.Email -> "email"
            AppleSignInRequestScope.FullName -> "name"
        }
    }
    return rememberOAuthState(
        provider = "apple.com",
        requestScopes = oathProviderRequestScopes,
        linkAccount = linkAccount,
        onResult = onResult,
    )
}
