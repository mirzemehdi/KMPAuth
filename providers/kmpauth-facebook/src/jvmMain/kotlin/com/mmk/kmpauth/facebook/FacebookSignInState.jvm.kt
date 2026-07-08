package com.mmk.kmpauth.facebook

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.mmk.kmpauth.core.KMPAuthInternalApi
import com.mmk.kmpauth.core.LaunchingSignInState
import com.mmk.kmpauth.core.SignInState
import com.mmk.kmpauth.core.logger.currentLogger

@OptIn(KMPAuthInternalApi::class)
@Composable
public actual fun rememberFacebookSignInState(
    requestScopes: List<FacebookSignInRequestScope>,
    linkAccount: Boolean,
    loginTracking: FacebookLoginTracking,
    onResult: (Result<FacebookUser>) -> Unit,
): SignInState {
    val scope = rememberCoroutineScope()
    return remember {
        LaunchingSignInState(scope) {
            currentLogger.log("Facebook Login is not supported on JVM")
        }
    }
}
