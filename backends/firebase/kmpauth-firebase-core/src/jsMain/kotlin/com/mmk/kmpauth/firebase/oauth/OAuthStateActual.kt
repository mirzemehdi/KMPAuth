package com.mmk.kmpauth.firebase.oauth

import androidx.compose.runtime.Composable
import com.mmk.kmpauth.core.SignInState
import com.mmk.kmpauth.core.auth.KMPAuthUser

@Composable
public actual fun rememberOAuthState(
    provider: String,
    requestScopes: List<String>,
    customParameters: Map<String, String>,
    linkAccount: Boolean,
    onResult: (Result<KMPAuthUser>) -> Unit,
): SignInState = rememberOAuthStateViaGitLive(
    provider = provider,
    requestScopes = requestScopes,
    customParameters = customParameters,
    linkAccount = linkAccount,
    onResult = onResult,
)
