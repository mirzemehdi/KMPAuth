package com.mmk.kmpauth.firebase.facebook

import androidx.compose.runtime.Composable
import com.mmk.kmpauth.core.SignInState
import com.mmk.kmpauth.facebook.FacebookLoginTracking
import com.mmk.kmpauth.facebook.FacebookSignInRequestScope
import dev.gitlive.firebase.auth.FirebaseUser

@Composable
public actual fun rememberFirebaseFacebookSignInState(
    requestScopes: List<FacebookSignInRequestScope>,
    linkAccount: Boolean,
    loginTracking: FacebookLoginTracking,
    onResult: (Result<FirebaseUser?>) -> Unit,
): SignInState = rememberFirebaseFacebookSignInStateInternal(
    requestScopes = requestScopes,
    linkAccount = linkAccount,
    loginTracking = loginTracking,
    onResult = onResult,
)
