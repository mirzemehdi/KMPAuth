package com.mmk.kmpauth.firebase.oauth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.mmk.kmpauth.core.KMPAuthInternalApi
import com.mmk.kmpauth.core.SignInState
import com.mmk.kmpauth.core.UnsupportedSignInState
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.OAuthProvider

@OptIn(KMPAuthInternalApi::class)
@Composable
internal actual fun rememberFirebaseGitLiveOAuthSignInState(
    oAuthProvider: OAuthProvider,
    linkAccount: Boolean,
    onResult: (Result<FirebaseUser?>) -> Unit,
): SignInState {
    val currentOnResult by rememberUpdatedState(onResult)
    return remember {
        UnsupportedSignInState(
            reason = "OAuth sign-in with Firebase is not implemented on the JS target yet.",
            onFailure = { currentOnResult(Result.failure(it)) },
        )
    }
}
