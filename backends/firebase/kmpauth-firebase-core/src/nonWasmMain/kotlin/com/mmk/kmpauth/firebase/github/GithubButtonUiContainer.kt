package com.mmk.kmpauth.firebase.github

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.mmk.kmpauth.core.UiContainerScope
import dev.gitlive.firebase.auth.FirebaseUser

/**
 * Legacy container API for Github Sign-In with Firebase. Superseded by
 * [rememberFirebaseGithubSignInState], which returns a
 * [com.mmk.kmpauth.core.SignInState] you can wire to any clickable without
 * the receiver-scope indirection.
 */
@Deprecated(
    "Use rememberFirebaseGithubSignInState(...) and call launch() from your own button's onClick. " +
        "Scheduled for removal in 4.0.",
    ReplaceWith(
        "rememberFirebaseGithubSignInState(requestScopes, customParameters, linkAccount, onResult)",
        "com.mmk.kmpauth.firebase.github.rememberFirebaseGithubSignInState"
    ),
    DeprecationLevel.WARNING
)
@Composable
public fun GithubButtonUiContainer(
    modifier: Modifier = Modifier,
    requestScopes: List<String> = listOf("user:email"),
    customParameters: Map<String, String> = emptyMap(),
    linkAccount: Boolean = false,
    onResult: (Result<FirebaseUser?>) -> Unit,
    content: @Composable UiContainerScope.() -> Unit,
) {
    val signInState = rememberFirebaseGithubSignInState(
        requestScopes = requestScopes,
        customParameters = customParameters,
        linkAccount = linkAccount,
        onResult = onResult,
    )
    val uiContainerScope = remember(signInState) {
        object : UiContainerScope {
            override fun onClick() = signInState.launch()
        }
    }
    Box(modifier = modifier) { uiContainerScope.content() }
}


@Deprecated(
    "Use GithubButtonUiContainer with linkAccount parameter, which defaults to false",
    ReplaceWith(""),
    DeprecationLevel.WARNING
)
@Composable
public fun GithubButtonUiContainer(
    modifier: Modifier = Modifier,
    requestScopes: List<String> = listOf("user:email"),
    customParameters: Map<String, String> = emptyMap(),
    onResult: (Result<FirebaseUser?>) -> Unit,
    content: @Composable UiContainerScope.() -> Unit,
) {
    @Suppress("DEPRECATION")
    GithubButtonUiContainer(
        modifier = modifier,
        requestScopes = requestScopes,
        linkAccount = false,
        customParameters = customParameters,
        onResult = onResult,
        content = content
    )
}
