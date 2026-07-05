package com.mmk.kmpauth.firebase.oauth

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.mmk.kmpauth.core.UiContainerScope
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.OAuthProvider

/**
 * Legacy container API for OAuth sign-in with Firebase. Superseded by
 * [rememberFirebaseOAuthSignInState], which returns a
 * [com.mmk.kmpauth.core.SignInState] you can wire to any clickable without
 * the receiver-scope indirection.
 */
@Deprecated(
    "Use rememberFirebaseOAuthSignInState(...) and call launch() from your own button's onClick. " +
        "Scheduled for removal in 4.0.",
    ReplaceWith(
        "rememberFirebaseOAuthSignInState(oAuthProvider, linkAccount, onResult)",
        "com.mmk.kmpauth.firebase.oauth.rememberFirebaseOAuthSignInState"
    ),
    DeprecationLevel.WARNING
)
@Composable
public fun OAuthContainer(
    modifier: Modifier = Modifier,
    oAuthProvider: OAuthProvider,
    onResult: (Result<FirebaseUser?>) -> Unit,
    linkAccount: Boolean = false,
    content: @Composable UiContainerScope.() -> Unit,
) {
    val signInState = rememberFirebaseOAuthSignInState(
        oAuthProvider = oAuthProvider,
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
    "Use OAuthContainer with linkAccount parameter, which defaults to false",
    ReplaceWith(""),
    DeprecationLevel.WARNING
)
@Composable
public fun OAuthContainer(
    modifier: Modifier = Modifier,
    oAuthProvider: OAuthProvider,
    onResult: (Result<FirebaseUser?>) -> Unit,
    content: @Composable UiContainerScope.() -> Unit,
) {
    @Suppress("DEPRECATION")
    OAuthContainer(
        modifier = modifier,
        oAuthProvider = oAuthProvider,
        onResult = onResult,
        linkAccount = false,
        content = content
    )
}
