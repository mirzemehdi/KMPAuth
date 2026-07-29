package com.mmk.kmpauth.core.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import com.mmk.kmpauth.core.KMPAuthInternalApi
import com.mmk.kmpauth.core.LaunchingSignInState
import com.mmk.kmpauth.core.SignInState

/**
 * Anonymous (guest) sign-in as a Compose state holder, served by the
 * registered auth backend (Firebase today; any [AuthProviderBackend]).
 * Creates (or resumes) a temporary account so users can try the app before
 * signing up.
 *
 * When an anonymous user is already signed in, launching again returns the
 * same user instead of creating a new account. To later upgrade the
 * anonymous user to a permanent account, sign in with any auth state using
 * `linkAccount = true` (e.g. [rememberEmailAuthState]) — the credential is
 * linked to the anonymous user, keeping its uid and data.
 *
 * ```
 * val anonymousAuth = rememberAnonymousAuthState(onResult = onAuthResult)
 *
 * Button(onClick = { anonymousAuth.launch() }) { Text("Continue as guest") }
 * ```
 *
 * With the Firebase backend: enable the "Anonymous" sign-in method in the
 * Firebase console; on Desktop (JVM) the underlying Firebase SDK does not
 * implement auth yet, and on wasm the SDK has no target — the flow reports
 * a failed [Result] there.
 *
 * @param onResult receives the signed-in anonymous [KMPAuthUser] or the
 * failure. The backend's native user stays reachable through
 * [KMPAuthUser.raw].
 */
@OptIn(KMPAuthInternalApi::class)
@Composable
public fun rememberAnonymousAuthState(
    onResult: (Result<KMPAuthUser>) -> Unit,
): SignInState {
    val scope = rememberCoroutineScope()
    val currentOnResult by rememberUpdatedState(onResult)

    return remember {
        LaunchingSignInState(scope) {
            currentOnResult(KMPAuthBackend.signInAnonymously())
        }
    }
}
