package com.mmk.kmpauth.firebase.facebook

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.mmk.kmpauth.core.UiContainerScope
import com.mmk.kmpauth.facebook.FacebookSignInRequestScope
import dev.gitlive.firebase.auth.FirebaseUser

/**
 * Legacy container API for Facebook Sign-In with Firebase. Superseded by
 * [rememberFirebaseFacebookSignInState], which returns a
 * [com.mmk.kmpauth.core.SignInState] you can wire to any clickable without
 * the receiver-scope indirection.
 */
@Deprecated(
    "Use rememberFirebaseFacebookSignInState(...) and call launch() from your own button's onClick. " +
        "Scheduled for removal in 4.0.",
    ReplaceWith(
        "rememberFirebaseFacebookSignInState(requestScopes, linkAccount, onResult)",
        "com.mmk.kmpauth.firebase.facebook.rememberFirebaseFacebookSignInState"
    ),
    DeprecationLevel.WARNING
)
@Composable
public fun FacebookButtonUiContainerFirebase(
    modifier: Modifier = Modifier,
    requestScopes: List<FacebookSignInRequestScope> = listOf(
        FacebookSignInRequestScope.PublicProfile,
        FacebookSignInRequestScope.Email
    ),
    onResult: (Result<FirebaseUser?>) -> Unit,
    linkAccount: Boolean = false,
    content: @Composable UiContainerScope.() -> Unit,
) {
    val signInState = rememberFirebaseFacebookSignInState(
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
