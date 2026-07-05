package com.mmk.kmpauth.firebase.oauth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.mmk.kmpauth.core.KMPAuthInternalApi
import com.mmk.kmpauth.core.LaunchingSignInState
import com.mmk.kmpauth.core.SignInState
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.OAuthProvider

@OptIn(KMPAuthInternalApi::class)
@Composable
public actual fun rememberFirebaseOAuthSignInState(
    oAuthProvider: OAuthProvider,
    linkAccount: Boolean,
    onResult: (Result<FirebaseUser?>) -> Unit,
): SignInState {
    val scope = rememberCoroutineScope()
    return remember {
        LaunchingSignInState(scope) {
            // OAuth sign-in with Firebase is not implemented on JS. The legacy
            // container was a no-op on this platform as well.
        }
    }
}
