package com.mmk.kmpauth.facebook

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.mmk.kmpauth.core.UiContainerScope

/**
 * Legacy container API for Facebook Sign-In. Superseded by
 * [rememberFacebookSignInState], which returns a
 * [com.mmk.kmpauth.core.SignInState] you can wire to any clickable without
 * the receiver-scope indirection.
 */
@Deprecated(
    "Use rememberFacebookSignInState(...) and call launch() from your own button's onClick. " +
        "Scheduled for removal in 4.0.",
    ReplaceWith(
        "rememberFacebookSignInState(requestScopes, linkAccount, onResult)",
        "com.mmk.kmpauth.facebook.rememberFacebookSignInState"
    ),
    DeprecationLevel.WARNING
)
@Composable
public fun FacebookButtonUiContainer(
    modifier: Modifier = Modifier,
    requestScopes: List<FacebookSignInRequestScope> = listOf(
        FacebookSignInRequestScope.PublicProfile,
        FacebookSignInRequestScope.Email
    ),
    onResult: (Result<FacebookUser>) -> Unit,
    linkAccount: Boolean = false,
    loginTracking: FacebookLoginTracking = FacebookLoginTracking.Limited,
    content: @Composable UiContainerScope.() -> Unit,
) {
    val signInState = rememberFacebookSignInState(
        requestScopes = requestScopes,
        linkAccount = linkAccount,
        loginTracking = loginTracking,
        onResult = onResult,
    )
    val uiContainerScope = remember(signInState) {
        object : UiContainerScope {
            override fun onClick() = signInState.launch()
        }
    }
    Box(modifier = modifier) { uiContainerScope.content() }
}
