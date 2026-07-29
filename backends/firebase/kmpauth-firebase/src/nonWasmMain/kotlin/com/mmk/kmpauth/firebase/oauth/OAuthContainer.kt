@file:OptIn(KMPAuthInternalApi::class)

package com.mmk.kmpauth.firebase.oauth

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import com.mmk.kmpauth.core.KMPAuthInternalApi
import com.mmk.kmpauth.core.LaunchingSignInState
import com.mmk.kmpauth.core.UiContainerScope
import com.mmk.kmpauth.core.runCatchingCancellable
import com.mmk.kmpauth.firebase.backend.gitLiveOAuthWebFlowSignIn
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.OAuthProvider

/**
 * Legacy container API for OAuth sign-in with Firebase. Superseded by
 * [rememberOAuthState], which returns a
 * [com.mmk.kmpauth.core.SignInState] you can wire to any clickable without
 * the receiver-scope indirection.
 */
@Deprecated(
    "Use rememberOAuthState(provider, ...) and call launch() from your own " +
        "button's onClick. Scheduled for removal in 4.0.",
    ReplaceWith(
        "rememberOAuthState(provider, requestScopes, customParameters, linkAccount, onResult)",
        "com.mmk.kmpauth.core.auth.rememberOAuthState"
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
    val scope = rememberCoroutineScope()
    val currentOAuthProvider by rememberUpdatedState(oAuthProvider)
    val currentLinkAccount by rememberUpdatedState(linkAccount)
    val currentOnResult by rememberUpdatedState(onResult)
    val signInState = remember {
        LaunchingSignInState(scope) {
            currentOnResult(runCatchingCancellable {
                gitLiveOAuthWebFlowSignIn(currentOAuthProvider, currentLinkAccount)
            })
        }
    }
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
