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
import com.mmk.kmpauth.core.runCatchingCancellable
import com.mmk.kmpauth.firebase.backend.FirebaseAuthBackend
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.EmailAuthProvider
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.auth

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
 * For password reset and passwordless email-link sign-in, see
 * [FirebaseEmailAuth].
 *
 * Note: on Desktop (JVM) the underlying Firebase SDK does not implement
 * auth yet, so the flow reports a failed [Result] there.
 *
 * @param email Email address, read at launch time.
 * @param password Password, read at launch time.
 * @param mode Whether launching signs in an existing account or creates a
 * new one. Default [EmailAuthMode.SignIn].
 * @param linkAccount true links the email/password credential to the
 * currently signed-in Firebase user instead of creating a new session —
 * e.g. to upgrade an anonymous user to a permanent account.
 * @param onResult receives the signed-in [FirebaseUser] or the failure
 * (wrong password, user not found, weak password, email already in use, ...).
 */
@OptIn(KMPAuthInternalApi::class)
@Composable
public fun rememberFirebaseEmailSignInState(
    email: String,
    password: String,
    mode: EmailAuthMode = EmailAuthMode.SignIn,
    linkAccount: Boolean = false,
    onResult: (Result<FirebaseUser?>) -> Unit,
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
                signInWithEmail(
                    email = currentEmail,
                    password = currentPassword,
                    mode = currentMode,
                    linkAccount = currentLinkAccount,
                )
            )
        }
    }
}

@OptIn(KMPAuthInternalApi::class)
private suspend fun signInWithEmail(
    email: String,
    password: String,
    mode: EmailAuthMode,
    linkAccount: Boolean,
): Result<FirebaseUser?> = runCatchingCancellable {
    val auth = Firebase.auth
    val currentUser = auth.currentUser
    val result = if (linkAccount && currentUser != null) {
        currentUser.linkWithCredential(EmailAuthProvider.credential(email, password))
    } else when (mode) {
        EmailAuthMode.SignIn -> auth.signInWithEmailAndPassword(email, password)
        EmailAuthMode.SignUp -> auth.createUserWithEmailAndPassword(email, password)
    }
    result.user
}
