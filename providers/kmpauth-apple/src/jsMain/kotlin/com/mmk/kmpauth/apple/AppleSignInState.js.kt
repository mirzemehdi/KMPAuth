package com.mmk.kmpauth.apple

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.mmk.kmpauth.core.KMPAuthInternalApi
import com.mmk.kmpauth.core.SignInState
import com.mmk.kmpauth.core.UnsupportedSignInState

@OptIn(KMPAuthInternalApi::class)
@Composable
public actual fun rememberAppleSignInState(
    requestScopes: List<AppleSignInRequestScope>,
    onResult: (Result<AppleUser>) -> Unit,
): SignInState {
    val currentOnResult by rememberUpdatedState(onResult)
    return remember {
        // Apple's native flow is Apple-platform only. The web OAuth flow
        // needs a server-side client-secret exchange, so it is not offered
        // here - use rememberAppleAuthState from kmpauth-firebase on
        // this platform.
        UnsupportedSignInState(
            reason = "Native Apple Sign-In is not supported on JS; " +
                "use rememberAppleAuthState (kmpauth-firebase) instead.",
            onFailure = { currentOnResult(Result.failure(it)) },
        )
    }
}
