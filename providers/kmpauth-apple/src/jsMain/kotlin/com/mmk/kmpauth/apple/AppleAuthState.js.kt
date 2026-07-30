package com.mmk.kmpauth.apple

import androidx.compose.runtime.Composable
import com.mmk.kmpauth.core.SignInState
import com.mmk.kmpauth.core.auth.KMPAuthUser

@Composable
public actual fun rememberAppleAuthState(
    requestScopes: List<AppleSignInRequestScope>,
    linkAccount: Boolean,
    onResult: (Result<KMPAuthUser>) -> Unit,
): SignInState = rememberAppleWebFlowAuthState(requestScopes, linkAccount, onResult)
