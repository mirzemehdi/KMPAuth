package com.mmk.kmpauth.firebase.facebook

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.mmk.kmpauth.core.KMPAuthInternalApi
import com.mmk.kmpauth.core.LaunchingSignInState
import com.mmk.kmpauth.core.SignInState
import com.mmk.kmpauth.core.logger.currentLogger
import com.mmk.kmpauth.facebook.FacebookLoginTracking
import com.mmk.kmpauth.facebook.FacebookSignInRequestScope
import dev.gitlive.firebase.auth.FirebaseUser

@OptIn(KMPAuthInternalApi::class)
@Composable
public actual fun rememberFirebaseFacebookSignInState(
    requestScopes: List<FacebookSignInRequestScope>,
    linkAccount: Boolean,
    loginTracking: FacebookLoginTracking,
    onResult: (Result<FirebaseUser?>) -> Unit,
): SignInState {
    val scope = rememberCoroutineScope()
    return remember {
        LaunchingSignInState(scope) {
            currentLogger.log("Facebook Login is not supported on JVM")
        }
    }
}
