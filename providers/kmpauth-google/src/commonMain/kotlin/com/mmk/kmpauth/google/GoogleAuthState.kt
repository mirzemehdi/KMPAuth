package com.mmk.kmpauth.google

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalInspectionMode
import com.mmk.kmpauth.core.KMPAuthInternalApi
import com.mmk.kmpauth.core.LaunchingSignInState
import com.mmk.kmpauth.core.NoOpSignInState
import com.mmk.kmpauth.core.SignInState
import com.mmk.kmpauth.core.auth.LocalKMPAuthBackend
import com.mmk.kmpauth.core.auth.KMPAuthUser

/**
 * Google Sign-In exchanged for a session by the registered auth backend
 * (Firebase today; any
 * [AuthProviderBackend][com.mmk.kmpauth.core.auth.AuthProviderBackend]),
 * as a Compose state holder. For the Google credential alone — no backend
 * session — use [rememberGoogleSignInState].
 *
 * Make sure [GoogleAuthProvider.create] was called at application start.
 * With `kmpauth-firebase` in the dependencies the Firebase backend
 * registers itself automatically; a custom backend is registered via
 * `KMPAuth.registerBackendProvider`.
 *
 * Parameters are read at launch time: recomposing with new values (e.g.
 * toggling [linkAccount] between sign-in and sign-up modes) updates the
 * existing state, and [SignInState.launch] uses whatever is current when
 * the user taps.
 *
 * ```
 * val googleAuth = rememberGoogleAuthState(onResult = onAuthResult)
 *
 * GoogleSignInButton(onClick = { googleAuth.launch() })
 * ```
 *
 * @param linkAccount true links the credential to the currently signed-in
 * user instead of creating a new session.
 * @param onResult receives the signed-in [KMPAuthUser] or the failure. The
 * backend's native user stays reachable through [KMPAuthUser.raw].
 */
@OptIn(KMPAuthInternalApi::class)
@Composable
public fun rememberGoogleAuthState(
    linkAccount: Boolean = false,
    filterByAuthorizedAccounts: Boolean = false,
    isAutoSelectEnabled: Boolean = true,
    scopes: List<String> = listOf("email", "profile"),
    onResult: (Result<KMPAuthUser>) -> Unit,
): SignInState {
    // IDE previews never run application startup, so GoogleAuthProvider.create()
    // has not been called and get() would throw. Render an inert state instead.
    if (LocalInspectionMode.current) return NoOpSignInState

    val googleAuthUiProvider = GoogleAuthProvider.get().getUiProvider()
    val scope = rememberCoroutineScope()
    val currentLinkAccount by rememberUpdatedState(linkAccount)
    val currentFilter by rememberUpdatedState(filterByAuthorizedAccounts)
    val currentAutoSelect by rememberUpdatedState(isAutoSelectEnabled)
    val currentScopes by rememberUpdatedState(scopes)
    val currentBackend by rememberUpdatedState(LocalKMPAuthBackend.current)
    val currentOnResult by rememberUpdatedState(onResult)

    return remember {
        LaunchingSignInState(scope) {
            val signInHandler = GoogleAuthSignInHandler(backend = currentBackend)
            val googleResult = googleAuthUiProvider.signIn(
                filterByAuthorizedAccounts = currentFilter,
                isAutoSelectEnabled = currentAutoSelect,
                scopes = currentScopes,
            )
            currentOnResult(
                googleResult.fold(
                    onSuccess = { googleUser ->
                        signInHandler.signIn(googleUser, currentLinkAccount)
                    },
                    // Propagate why Google sign-in failed instead of reporting a
                    // generic "id token is null" from the Firebase exchange.
                    onFailure = { error -> Result.failure(error) },
                )
            )
        }
    }
}
