package com.mmk.kmpauth.firebase.google

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import com.mmk.kmpauth.core.KMPAuthInternalApi
import com.mmk.kmpauth.core.LaunchingSignInState
import com.mmk.kmpauth.core.SignInState
import com.mmk.kmpauth.core.auth.KMPAuthBackend
import com.mmk.kmpauth.firebase.backend.FirebaseAuthBackend
import com.mmk.kmpauth.google.GoogleAuthProvider
import dev.gitlive.firebase.auth.FirebaseUser

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
 * @param onResult receives the signed-in [FirebaseUser] or the failure.
 */
@OptIn(KMPAuthInternalApi::class)
@Composable
public fun rememberFirebaseGoogleSignInState(
    linkAccount: Boolean = false,
    filterByAuthorizedAccounts: Boolean = false,
    isAutoSelectEnabled: Boolean = true,
    scopes: List<String> = listOf("email", "profile"),
    onResult: (Result<FirebaseUser?>) -> Unit,
): SignInState {
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
            val googleUser = googleAuthUiProvider.signIn(
                filterByAuthorizedAccounts = currentFilter,
                isAutoSelectEnabled = currentAutoSelect,
                scopes = currentScopes,
            )
            currentOnResult(signInHandler.signIn(googleUser, currentLinkAccount))
        }
    }
}
