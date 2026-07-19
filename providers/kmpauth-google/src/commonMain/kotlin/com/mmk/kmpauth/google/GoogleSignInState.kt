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
import com.mmk.kmpauth.core.logger.currentLogger
import com.mmk.kmpauth.google.GoogleAuthUiProvider.Companion.BASIC_AUTH_SCOPE

/**
 * Google Sign-In (without any backend) as a Compose state holder. Make sure
 * [GoogleAuthProvider.create] was called at application start.
 *
 * Parameters are read at launch time: recomposing with new values (e.g.
 * toggling between sign-in and sign-up modes) updates the existing state,
 * and [SignInState.launch] uses whatever is current when the user taps.
 *
 * ```
 * val googleSignIn = rememberGoogleSignInState(onResult = { googleUser ->
 *     val idToken = googleUser?.idToken // send to your backend
 * })
 *
 * Button(onClick = { googleSignIn.launch() }) { Text("Google Sign-In") }
 * ```
 *
 * @param filterByAuthorizedAccounts true limits the account list to accounts
 * previously used with this app; false lists all available accounts.
 * @param isAutoSelectEnabled sign in automatically when exactly one eligible
 * account exists.
 * @param scopes OAuth scopes to request. Default `listOf("email", "profile")`.
 * @param onResult receives the [GoogleUser], or null when sign-in fails.
 */
@OptIn(KMPAuthInternalApi::class)
@Composable
public fun rememberGoogleSignInState(
    filterByAuthorizedAccounts: Boolean = false,
    isAutoSelectEnabled: Boolean = true,
    scopes: List<String> = BASIC_AUTH_SCOPE,
    onResult: (GoogleUser?) -> Unit,
): SignInState {
    // IDE previews never run application startup, so GoogleAuthProvider.create()
    // has not been called and get() would throw. Render an inert state instead.
    if (LocalInspectionMode.current) return NoOpSignInState

    val googleAuthUiProvider = GoogleAuthProvider.get().getUiProvider()
    val scope = rememberCoroutineScope()
    val currentFilter by rememberUpdatedState(filterByAuthorizedAccounts)
    val currentAutoSelect by rememberUpdatedState(isAutoSelectEnabled)
    val currentScopes by rememberUpdatedState(scopes)
    val currentOnResult by rememberUpdatedState(onResult)

    return remember {
        LaunchingSignInState(scope) {
            currentLogger.log("Google sign-in launched")
            val googleUser = googleAuthUiProvider.signIn(
                filterByAuthorizedAccounts = currentFilter,
                isAutoSelectEnabled = currentAutoSelect,
                scopes = currentScopes,
            )
            currentOnResult(googleUser)
        }
    }
}
