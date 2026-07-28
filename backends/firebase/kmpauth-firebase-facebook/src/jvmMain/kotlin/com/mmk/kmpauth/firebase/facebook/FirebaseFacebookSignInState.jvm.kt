package com.mmk.kmpauth.firebase.facebook

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.mmk.kmpauth.core.KMPAuthInternalApi
import com.mmk.kmpauth.core.SignInState
import com.mmk.kmpauth.core.UnsupportedSignInState
import com.mmk.kmpauth.core.auth.KMPAuthUser
import com.mmk.kmpauth.facebook.FacebookLoginTracking
import com.mmk.kmpauth.facebook.FacebookSignInRequestScope

@OptIn(KMPAuthInternalApi::class)
@Composable
public actual fun rememberFirebaseFacebookSignInState(
    requestScopes: List<FacebookSignInRequestScope>,
    linkAccount: Boolean,
    loginTracking: FacebookLoginTracking,
    onResult: (Result<KMPAuthUser?>) -> Unit,
): SignInState {
    val currentOnResult by rememberUpdatedState(onResult)
    return remember {
        UnsupportedSignInState(
            reason = "Facebook Sign-In is not supported on Desktop: the Facebook " +
                "SDK has no JVM target.",
            onFailure = { currentOnResult(Result.failure(it)) },
        )
    }
}
