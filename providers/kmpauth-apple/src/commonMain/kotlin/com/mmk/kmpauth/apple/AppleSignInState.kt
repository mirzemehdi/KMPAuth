package com.mmk.kmpauth.apple

import androidx.compose.runtime.Composable
import com.mmk.kmpauth.core.SignInState

/**
 * Native Sign in with Apple (without any backend) as a Compose state holder.
 *
 * Parameters are read at launch time: recomposing with new values updates the
 * existing state, and [SignInState.launch] uses whatever is current when the
 * user taps.
 *
 * ```
 * val appleSignIn = rememberAppleSignInState(onResult = { result ->
 *     val idToken = result.getOrNull()?.idToken // send to your backend
 * })
 *
 * AppleSignInButton(onClick = { appleSignIn.launch() })
 * ```
 *
 * **Apple platforms only.** The native flow exists only on iOS, where Apple
 * returns an identity token your backend can verify on its own. On Android,
 * JVM, JS and wasmJs this state is a no-op that logs and never produces a
 * result - Apple's web OAuth flow returns an authorization code that must be
 * exchanged with a client secret server-side, which is not safe to do from a
 * client. Use `rememberFirebaseAppleSignInState` from `kmpauth-firebase`
 * if you need Apple Sign-In on non-Apple targets.
 *
 * @param requestScopes Scopes requested from the user. Defaults to full name
 * and email. Apple returns these values **only on the first authorization**.
 * @param onResult receives the signed-in [AppleUser] or the failure.
 */
@Composable
public expect fun rememberAppleSignInState(
    requestScopes: List<AppleSignInRequestScope> = listOf(
        AppleSignInRequestScope.FullName,
        AppleSignInRequestScope.Email
    ),
    onResult: (Result<AppleUser>) -> Unit,
): SignInState
