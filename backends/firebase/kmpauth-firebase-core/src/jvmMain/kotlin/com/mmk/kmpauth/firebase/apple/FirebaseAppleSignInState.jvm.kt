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
public actual fun rememberAppleAuthState(
    requestScopes: List<AppleSignInRequestScope>,
    linkAccount: Boolean,
    onResult: (Result<KMPAuthUser>) -> Unit,
): SignInState {
    val currentOnResult by rememberUpdatedState(onResult)
    return remember {
        UnsupportedSignInState(
            reason = "Apple Sign-In's web flow is not available on Desktop yet " +
                "(https://github.com/mirzemehdi/KMPAuth/issues/81). If you obtain an " +
                "Apple identity token elsewhere, exchange it via " +
                "KMPAuth.signIn(AuthCredential.IdToken(...)).",
            onFailure = { currentOnResult(Result.failure(it)) },
        )
    }
}
