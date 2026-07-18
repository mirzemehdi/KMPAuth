package com.mmk.kmpauth.firebase.apple

import com.mmk.kmpauth.apple.AppleSignInRequestScope
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.mmk.kmpauth.core.UiContainerScope
import dev.gitlive.firebase.auth.FirebaseUser

/**
 * Legacy container API for Apple Sign-In with Firebase. Superseded by
 * [rememberFirebaseAppleSignInState], which returns a
 * [com.mmk.kmpauth.core.SignInState] you can wire to any clickable without
 * the receiver-scope indirection.
 */
@Deprecated(
    "Use rememberFirebaseAppleSignInState(...) and call launch() from your own button's onClick. " +
        "Scheduled for removal in 4.0.",
    ReplaceWith(
        "rememberFirebaseAppleSignInState(requestScopes, linkAccount, onResult)",
        "com.mmk.kmpauth.firebase.apple.rememberFirebaseAppleSignInState"
    ),
    DeprecationLevel.WARNING
)
@Composable
public fun AppleButtonUiContainer(
    modifier: Modifier = Modifier,
    requestScopes: List<AppleSignInRequestScope> = listOf(
        AppleSignInRequestScope.FullName,
        AppleSignInRequestScope.Email
    ),
    onResult: (Result<FirebaseUser?>) -> Unit,
    linkAccount: Boolean = false,
    content: @Composable UiContainerScope.() -> Unit,
) {
    val signInState = rememberFirebaseAppleSignInState(
        requestScopes = requestScopes,
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
    "Use AppleButtonUiContainer with the linkAccount parameter, which defaults to false.",
    ReplaceWith(""),
    DeprecationLevel.WARNING
)
@Composable
public fun AppleButtonUiContainer(
    modifier: Modifier = Modifier,
    requestScopes: List<AppleSignInRequestScope> = listOf(
        AppleSignInRequestScope.FullName,
        AppleSignInRequestScope.Email
    ),
    onResult: (Result<FirebaseUser?>) -> Unit,
    content: @Composable UiContainerScope.() -> Unit,
) {
    @Suppress("DEPRECATION")
    AppleButtonUiContainer(
        modifier = modifier,
        requestScopes = requestScopes,
        onResult = onResult,
        linkAccount = false,
        content = content
    )
}
