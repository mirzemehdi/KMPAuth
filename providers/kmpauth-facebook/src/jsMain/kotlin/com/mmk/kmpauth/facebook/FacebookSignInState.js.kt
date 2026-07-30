package com.mmk.kmpauth.facebook

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.mmk.kmpauth.core.KMPAuthInternalApi
import com.mmk.kmpauth.core.SignInState
import com.mmk.kmpauth.core.UnsupportedSignInState

@OptIn(KMPAuthInternalApi::class)
@Composable
public actual fun rememberFacebookSignInState(
    requestScopes: List<FacebookSignInRequestScope>,
    linkAccount: Boolean,
    loginTracking: FacebookLoginTracking,
    onResult: (Result<FacebookUser>) -> Unit,
): SignInState {
    val currentOnResult by rememberUpdatedState(onResult)
    return remember {
        // Meta ships its Login SDK for Android and iOS only; on other
        // platforms use Facebook as a browser OAuth provider instead
        // (Firebase's web flow, or Supabase's OAuthWebFlow("facebook.com")).
        UnsupportedSignInState(
            reason = "Facebook Login (native SDK) is not supported on JS; " +
                "use the backend browser OAuth flow instead: " +
                "rememberOAuthState(\"facebook.com\") - on web currently served " +
                "by the Supabase backend (the Firebase web flow is not " +
                "implemented on JS yet).",
            onFailure = { currentOnResult(Result.failure(it)) },
        )
    }
}
