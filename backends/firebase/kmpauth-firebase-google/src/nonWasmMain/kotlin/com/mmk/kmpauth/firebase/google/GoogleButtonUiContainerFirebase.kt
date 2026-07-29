package com.mmk.kmpauth.firebase.google

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.mmk.kmpauth.core.UiContainerScope
import com.mmk.kmpauth.core.auth.KMPAuthBackend
import com.mmk.kmpauth.firebase.backend.FirebaseAuthBackend
import com.mmk.kmpauth.google.rememberGoogleAuthState
import dev.gitlive.firebase.auth.FirebaseUser

/**
 * Legacy container API for Google Sign-In with Firebase. Superseded by
 * [rememberGoogleAuthState], which returns a
 * [com.mmk.kmpauth.core.SignInState] you can wire to any clickable without
 * the receiver-scope indirection.
 */
@Deprecated(
    "Use rememberGoogleAuthState(...) and call launch() from your own button's onClick. " +
        "Scheduled for removal in 4.0.",
    ReplaceWith(
        "rememberGoogleAuthState(linkAccount, filterByAuthorizedAccounts, isAutoSelectEnabled, scopes, onResult)",
        "com.mmk.kmpauth.google.rememberGoogleAuthState"
    ),
    DeprecationLevel.WARNING
)
@Composable
public fun GoogleButtonUiContainerFirebase(
    modifier: Modifier = Modifier,
    linkAccount: Boolean = false,
    filterByAuthorizedAccounts: Boolean = false,
    isAutoSelectEnabled: Boolean = true,
    scopes: List<String> = listOf("email", "profile"),
    onResult: (Result<FirebaseUser?>) -> Unit,
    content: @Composable UiContainerScope.() -> Unit,
) {
    // 2.x apps never registered a backend explicitly; keep the container
    // zero-config by lazily registering the Firebase default.
    KMPAuthBackend.register(FirebaseAuthBackend)
    val signInState = rememberGoogleAuthState(
        linkAccount = linkAccount,
        filterByAuthorizedAccounts = filterByAuthorizedAccounts,
        isAutoSelectEnabled = isAutoSelectEnabled,
        scopes = scopes,
        // The state reports KMPAuthUser; this 2.x-compat container keeps its
        // Result<FirebaseUser?> callback by unwrapping the native user.
        onResult = { result -> onResult(result.map { it.raw as? FirebaseUser }) },
    )
    val uiContainerScope = remember(signInState) {
        object : UiContainerScope {
            override fun onClick() = signInState.launch()
        }
    }
    Box(modifier = modifier) { uiContainerScope.content() }
}

@Deprecated(
    "Use rememberGoogleAuthState(...) and call launch() from your own button's onClick. " +
        "Scheduled for removal in 4.0.",
    ReplaceWith(
        "rememberGoogleAuthState(onResult = onResult)",
        "com.mmk.kmpauth.google.rememberGoogleAuthState"
    ),
    DeprecationLevel.WARNING
)
@Composable
public fun GoogleButtonUiContainerFirebase(
    modifier: Modifier = Modifier,
    onResult: (Result<FirebaseUser?>) -> Unit,
    content: @Composable UiContainerScope.() -> Unit,
) {
    @Suppress("DEPRECATION")
    GoogleButtonUiContainerFirebase(
        modifier = modifier,
        linkAccount = false,
        filterByAuthorizedAccounts = false,
        onResult = onResult,
        content = content
    )
}
