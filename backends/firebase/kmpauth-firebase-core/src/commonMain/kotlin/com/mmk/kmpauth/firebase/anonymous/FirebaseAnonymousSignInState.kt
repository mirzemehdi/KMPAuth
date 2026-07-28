package com.mmk.kmpauth.firebase.anonymous

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import com.mmk.kmpauth.core.KMPAuthInternalApi
import com.mmk.kmpauth.core.LaunchingSignInState
import com.mmk.kmpauth.core.SignInState
import com.mmk.kmpauth.core.auth.KMPAuthBackend
import com.mmk.kmpauth.core.auth.KMPAuthUser
import com.mmk.kmpauth.firebase.backend.FirebaseAuthBackend

/**
 * Anonymous Firebase sign-in as a Compose state holder. Creates (or
 * resumes) a temporary account so users can try the app before signing up.
 *
 * When an anonymous user is already signed in, launching again returns the
 * same user instead of creating a new account. To later upgrade the
 * anonymous user to a permanent account, sign in with any provider state
 * using `linkAccount = true` (e.g.
 * [com.mmk.kmpauth.firebase.email.rememberFirebaseEmailSignInState]) —
 * the credential is linked to the anonymous user, keeping its uid and data.
 *
 * ```
 * val anonymousSignIn = rememberFirebaseAnonymousSignInState(onResult = onFirebaseResult)
 *
 * Button(onClick = { anonymousSignIn.launch() }) { Text("Continue as guest") }
 * ```
 *
 * Enable the "Anonymous" sign-in method in the Firebase console first.
 *
 * Note: on Desktop (JVM) the underlying Firebase SDK does not implement
 * auth yet, and on wasm the SDK has no target — the flow reports a failed
 * [Result] there.
 *
 * @param onResult receives the signed-in anonymous [KMPAuthUser] or the
 * failure. The native Firebase user stays reachable through [KMPAuthUser.raw].
 */
@OptIn(KMPAuthInternalApi::class)
@Composable
public fun rememberFirebaseAnonymousSignInState(
    onResult: (Result<KMPAuthUser?>) -> Unit,
): SignInState {
    val scope = rememberCoroutineScope()
    val currentOnResult by rememberUpdatedState(onResult)

    return remember {
        // Lazy default registration: no-op when the app already registered
        // a backend at startup (first registration wins).
        KMPAuthBackend.register(FirebaseAuthBackend)
        LaunchingSignInState(scope) {
            currentOnResult(firebaseAnonymousSignIn())
        }
    }
}

/**
 * Platform anonymous sign-in: delegates to the Firebase SDK where it
 * exists; reports an unsupported failure on wasm.
 */
internal expect suspend fun firebaseAnonymousSignIn(): Result<KMPAuthUser?>
