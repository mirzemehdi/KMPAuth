package com.mmk.kmpauth.firebase.apple

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.mmk.kmpauth.core.KMPAuthInternalApi
import com.mmk.kmpauth.core.LaunchingSignInState
import com.mmk.kmpauth.core.SignInState
import dev.gitlive.firebase.auth.FirebaseUser

@OptIn(KMPAuthInternalApi::class)
@Composable
public actual fun rememberFirebaseAppleSignInState(
    requestScopes: List<AppleSignInRequestScope>,
    linkAccount: Boolean,
    onResult: (Result<FirebaseUser?>) -> Unit,
): SignInState {
    val scope = rememberCoroutineScope()
    return remember {
        LaunchingSignInState(scope) {
            // Apple Sign-In with Firebase is not implemented on JVM. The legacy
            // container was a no-op on this platform as well.
        }
    }
}
