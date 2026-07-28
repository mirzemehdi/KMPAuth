package com.mmk.kmpauth.firebase.apple

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.mmk.kmpauth.apple.AppleSignInRequestScope
import com.mmk.kmpauth.core.KMPAuthInternalApi
import com.mmk.kmpauth.core.SignInState
import com.mmk.kmpauth.core.UnsupportedSignInState
import com.mmk.kmpauth.core.auth.KMPAuthUser

@OptIn(KMPAuthInternalApi::class)
@Composable
public actual fun rememberFirebaseAppleSignInState(
    requestScopes: List<AppleSignInRequestScope>,
    linkAccount: Boolean,
    onResult: (Result<KMPAuthUser?>) -> Unit,
): SignInState {
    val currentOnResult by rememberUpdatedState(onResult)
    return remember {
        UnsupportedSignInState(
            reason = "Apple Sign-In with Firebase is not implemented on Desktop " +
                "(https://github.com/mirzemehdi/KMPAuth/issues/204).",
            onFailure = { currentOnResult(Result.failure(it)) },
        )
    }
}
