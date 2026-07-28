package com.mmk.kmpauth.firebase.email

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
 * Whether an email/password launch signs in an existing user or creates a
 * new account. See [rememberFirebaseEmailSignInState].
 */
public enum class EmailAuthMode {
    /** Sign in an existing account (`signInWithEmailAndPassword`). */
    SignIn,

    /** Create a new account (`createUserWithEmailAndPassword`). */
    SignUp,
}

/**
 * Email/password authentication with Firebase as a Compose state holder.
 *
 * Parameters are read at launch time: pass the current values of your email
 * and password fields, and [SignInState.launch] uses whatever is current
 * when the user taps — no callback wiring needed as the user types.
 *
 * ```
 * var email by remember { mutableStateOf("") }
 * var password by remember { mutableStateOf("") }
 * val emailSignIn = rememberFirebaseEmailSignInState(
 *     email = email,
 *     password = password,
 *     onResult = onFirebaseResult,
 * )
 *
 * Button(onClick = { emailSignIn.launch() }, enabled = !emailSignIn.isInProgress) {
 *     Text("Sign in with email")
 * }
 * ```
 *
 * For password reset, reauthentication and passwordless email-link sign-in,
 * see [FirebaseEmailAuth].
 *
 * Note: on Desktop (JVM) the underlying Firebase SDK does not implement
 * auth yet, and on wasm the SDK has no target — the flow reports a failed
 * [Result] there.
 *
 * @param email Email address, read at launch time.
 * @param password Password, read at launch time.
 * @param mode Whether launching signs in an existing account or creates a
 * new one. Default [EmailAuthMode.SignIn].
 * @param linkAccount true links the email/password credential to the
 * currently signed-in Firebase user instead of creating a new session —
 * e.g. to upgrade an anonymous user to a permanent account.
 * @param onResult receives the signed-in [KMPAuthUser] or the failure
 * (wrong password, user not found, weak password, email already in use, ...).
 * The native Firebase user stays reachable through [KMPAuthUser.raw].
 */
@OptIn(KMPAuthInternalApi::class)
@Composable
public fun rememberFirebaseEmailSignInState(
    email: String,
    password: String,
    mode: EmailAuthMode = EmailAuthMode.SignIn,
    linkAccount: Boolean = false,
    onResult: (Result<KMPAuthUser?>) -> Unit,
): SignInState {
    val scope = rememberCoroutineScope()
    val currentEmail by rememberUpdatedState(email)
    val currentPassword by rememberUpdatedState(password)
    val currentMode by rememberUpdatedState(mode)
    val currentLinkAccount by rememberUpdatedState(linkAccount)
    val currentOnResult by rememberUpdatedState(onResult)

    return remember {
        // Lazy default registration: no-op when the app already registered
        // a backend at startup (first registration wins).
        KMPAuthBackend.register(FirebaseAuthBackend)
        LaunchingSignInState(scope) {
            currentOnResult(
                firebaseEmailSignIn(
                    email = currentEmail,
                    password = currentPassword,
                    mode = currentMode,
                    linkAccount = currentLinkAccount,
                )
            )
        }
    }
}

/**
 * Platform email/password exchange: delegates to the Firebase SDK where it
 * exists; reports an unsupported failure on wasm.
 */
internal expect suspend fun firebaseEmailSignIn(
    email: String,
    password: String,
    mode: EmailAuthMode,
    linkAccount: Boolean,
): Result<KMPAuthUser?>
