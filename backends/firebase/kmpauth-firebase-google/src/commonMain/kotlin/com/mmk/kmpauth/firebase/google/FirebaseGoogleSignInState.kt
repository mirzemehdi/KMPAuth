package com.mmk.kmpauth.firebase.google

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
import com.mmk.kmpauth.core.auth.KMPAuthBackend
import com.mmk.kmpauth.firebase.backend.FirebaseAuthBackend
import com.mmk.kmpauth.google.GoogleAuthProvider
import com.mmk.kmpauth.core.auth.KMPAuthUser

/**
 * Google Sign-In exchanged for a Firebase session, as a Compose state
 * holder. Make sure [GoogleAuthProvider.create] was called at application
 * start.
 *
 * Parameters are read at launch time: recomposing with new values (e.g.
 * toggling [linkAccount] between sign-in and sign-up modes) updates the
 * existing state, and [SignInState.launch] uses whatever is current when
 * the user taps.
 *
 * ```
 * val googleSignIn = rememberFirebaseGoogleSignInState(onResult = onFirebaseResult)
 *
 * GoogleSignInButton(onClick = { googleSignIn.launch() })
 * ```
 *
 * @param linkAccount true links the credential to the currently signed-in
 * Firebase user instead of creating a new session.
 * @param onResult receives the signed-in [KMPAuthUser] or the failure. The
 * native Firebase user stays reachable through [KMPAuthUser.raw].
 */
@OptIn(KMPAuthInternalApi::class)
@Composable
public fun rememberFirebaseGoogleSignInState(
    linkAccount: Boolean = false,
    filterByAuthorizedAccounts: Boolean = false,
    isAutoSelectEnabled: Boolean = true,
    scopes: List<String> = listOf("email", "profile"),
    onResult: (Result<KMPAuthUser?>) -> Unit,
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
    val currentOnResult by rememberUpdatedState(onResult)

    return remember {
        // Lazy default registration: no-op when the app already registered
        // a backend at startup (first registration wins).
        KMPAuthBackend.register(FirebaseAuthBackend)
        val signInHandler = GoogleFirebaseSignInHandler(backend = KMPAuthBackend.require())
        LaunchingSignInState(scope) {
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
