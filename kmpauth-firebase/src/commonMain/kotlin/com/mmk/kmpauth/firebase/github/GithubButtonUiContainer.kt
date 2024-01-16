package com.mmk.kmpauth.firebase.github

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mmk.kmpauth.core.UiContainerScope
import com.mmk.kmpauth.firebase.oauth.OAuthContainer
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.OAuthProvider

@Composable
public fun GithubButtonUiContainer(
    modifier: Modifier = Modifier,
    requestScopes: List<String> = listOf("user:email"),
    customParameters: Map<String, String> = emptyMap(),
    onResult: (Result<FirebaseUser?>) -> Unit,
    content: @Composable UiContainerScope.() -> Unit,
) {
    val oAuthProvider = OAuthProvider(
        provider = "github.com",
        scopes = requestScopes,
        customParameters = customParameters
    )
    OAuthContainer(
        modifier = modifier,
        oAuthProvider = oAuthProvider,
        onResult = onResult,
        content = content
    )

}