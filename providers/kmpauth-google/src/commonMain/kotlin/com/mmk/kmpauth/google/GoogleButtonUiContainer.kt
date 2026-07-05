package com.mmk.kmpauth.google

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.mmk.kmpauth.core.UiContainerScope
import com.mmk.kmpauth.google.GoogleAuthUiProvider.Companion.BASIC_AUTH_SCOPE

/**
 * Legacy container API for Google Sign-In. Superseded by
 * [rememberGoogleSignInState], which returns a [com.mmk.kmpauth.core.SignInState]
 * you can wire to any clickable without the receiver-scope indirection.
 */
@Deprecated(
    "Use rememberGoogleSignInState(...) and call launch() from your own button's onClick. " +
        "Scheduled for removal in 4.0.",
    ReplaceWith(
        "rememberGoogleSignInState(filterByAuthorizedAccounts, isAutoSelectEnabled, scopes, onGoogleSignInResult)",
        "com.mmk.kmpauth.google.rememberGoogleSignInState"
    ),
    DeprecationLevel.WARNING
)
@Composable
public fun GoogleButtonUiContainer(
    modifier: Modifier = Modifier,
    filterByAuthorizedAccounts: Boolean = false,
    isAutoSelectEnabled: Boolean = true,
    scopes: List<String> = BASIC_AUTH_SCOPE,
    onGoogleSignInResult: (GoogleUser?) -> Unit,
    content: @Composable UiContainerScope.() -> Unit,
) {
    val signInState = rememberGoogleSignInState(
        filterByAuthorizedAccounts = filterByAuthorizedAccounts,
        isAutoSelectEnabled = isAutoSelectEnabled,
        scopes = scopes,
        onResult = onGoogleSignInResult,
    )
    val uiContainerScope = remember(signInState) {
        object : UiContainerScope {
            override fun onClick() = signInState.launch()
        }
    }
    Box(modifier = modifier) { uiContainerScope.content() }
}
