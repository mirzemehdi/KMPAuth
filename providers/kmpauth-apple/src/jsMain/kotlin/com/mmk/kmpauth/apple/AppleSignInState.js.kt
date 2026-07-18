package com.mmk.kmpauth.apple

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.mmk.kmpauth.core.KMPAuthInternalApi
import com.mmk.kmpauth.core.LaunchingSignInState
import com.mmk.kmpauth.core.SignInState
import com.mmk.kmpauth.core.logger.currentLogger

@OptIn(KMPAuthInternalApi::class)
@Composable
public actual fun rememberAppleSignInState(
    requestScopes: List<AppleSignInRequestScope>,
    onResult: (Result<AppleUser>) -> Unit,
): SignInState {
    val scope = rememberCoroutineScope()
    return remember {
        LaunchingSignInState(scope) {
            // Apple's native flow is Apple-platform only. The web OAuth flow
            // needs a server-side client-secret exchange, so it is not offered
            // here - use rememberFirebaseAppleSignInState from
            // kmpauth-firebase-core on this platform.
            currentLogger.log("Native Apple Sign-In is not supported on JS")
        }
    }
}
