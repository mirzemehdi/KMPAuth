package com.mmk.kmpauth.firebase.apple

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mmk.kmpauth.core.UiContainerScope
import com.mmk.kmpauth.firebase.oauth.OAuthContainer
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.OAuthProvider

@Composable
public actual fun AppleButtonUiContainer(
    modifier: Modifier,
    requestScopes: List<AppleSignInRequestScope>,
    onResult: (Result<FirebaseUser?>) -> Unit,
    content: @Composable UiContainerScope.() -> Unit,
) {
    val oathProviderRequestScopes = requestScopes.map {
        when (it) {
            AppleSignInRequestScope.Email -> "email"
            AppleSignInRequestScope.FullName -> "name"
        }
    }
    val oAuthProvider = OAuthProvider(provider = "apple.com", scopes = oathProviderRequestScopes)
    OAuthContainer(oAuthProvider = oAuthProvider, onResult =  onResult, content = content)
}