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
 * Whether an email/password launch signs in an existing user or creates a
 * new account. See [rememberEmailAuthState].
 */
public enum class EmailAuthMode {
    /** Sign in an existing account. */
    SignIn,

    /** Create a new account. */
    SignUp,
}

/**
 * Email/password authentication as a Compose state holder, served by the
 * registered auth backend (Firebase today; any [AuthProviderBackend]).
 *
 * Parameters are read at launch time: pass the current values of your email
 * and password fields, and [SignInState.launch] uses whatever is current
 * when the user taps — no callback wiring needed as the user types.
 *
 * ```
 * var email by remember { mutableStateOf("") }
 * var password by remember { mutableStateOf("") }
 * val emailAuth = rememberEmailAuthState(
 *     email = email,
 *     password = password,
 *     onResult = onAuthResult,
 * )
 *
 * Button(onClick = { emailAuth.launch() }, enabled = !emailAuth.isInProgress) {
 *     Text("Sign in with email")
 * }
 * ```
 *
 * Password reset, reauthentication and passwordless email-link sign-in are
 * suspend operations on [com.mmk.kmpauth.core.KMPAuth]:
 *
 * ```
 * KMPAuth.sendPasswordResetEmail(email)
 * KMPAuth.reauthenticate(AuthCredential.EmailPassword(email, password))
 * ```
 *
 * With the Firebase backend: enable the "Email/Password" sign-in method in
 * the Firebase console. Works on all targets including Desktop (JVM, via
 * the Firebase REST API - call GitLive's `Firebase.initialize` there);
 * on wasm the flow reports a failed [Result].
 *
 * @param email Email address, read at launch time.
 * @param password Password, read at launch time.
 * @param mode Whether launching signs in an existing account or creates a
 * new one. Default [EmailAuthMode.SignIn].
 * @param linkAccount true links the email/password credential to the
 * currently signed-in user instead of creating a new session — e.g. to
 * upgrade an anonymous user to a permanent account.
 * @param backend serves this state. Defaults to the registered process-wide
 * backend ([KMPAuthBackend]); pass a specific [AuthProviderBackend] instance
 * to use several backends side by side (e.g. Firebase and Supabase).
 * @param onResult receives the signed-in [KMPAuthUser] or the failure
 * (wrong password, user not found, weak password, email already in use, ...).
 * The backend's native user stays reachable through [KMPAuthUser.raw].
 */
@OptIn(KMPAuthInternalApi::class)
@Composable
public fun rememberEmailAuthState(
    email: String,
    password: String,
    mode: EmailAuthMode = EmailAuthMode.SignIn,
    linkAccount: Boolean = false,
    backend: AuthProviderBackend = KMPAuthBackend,
    onResult: (Result<KMPAuthUser>) -> Unit,
): SignInState {
    val scope = rememberCoroutineScope()
    val currentEmail by rememberUpdatedState(email)
    val currentPassword by rememberUpdatedState(password)
    val currentMode by rememberUpdatedState(mode)
    val currentLinkAccount by rememberUpdatedState(linkAccount)
    val currentBackend by rememberUpdatedState(backend)
    val currentOnResult by rememberUpdatedState(onResult)

    return remember {
        LaunchingSignInState(scope) {
            val result = when {
                // Linking uses the credential exchange so the anonymous
                // user's uid and data are kept.
                currentLinkAccount || currentMode == EmailAuthMode.SignIn ->
                    currentBackend.signIn(
                        credential = AuthCredential.EmailPassword(currentEmail, currentPassword),
                        linkWithCurrentUser = currentLinkAccount,
                    )

                else -> currentBackend.signUp(currentEmail, currentPassword)
            }
            currentOnResult(result)
        }
    }
}
