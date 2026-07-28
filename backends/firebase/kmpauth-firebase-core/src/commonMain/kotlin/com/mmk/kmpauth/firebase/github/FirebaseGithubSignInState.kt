package com.mmk.kmpauth.firebase.github

import androidx.compose.runtime.Composable
import com.mmk.kmpauth.core.SignInState
import com.mmk.kmpauth.core.auth.KMPAuthUser
import com.mmk.kmpauth.firebase.oauth.rememberFirebaseOAuthSignInState

/**
 * Github Sign-In with Firebase as a Compose state holder.
 *
 * Parameters are read at launch time: recomposing with new values (e.g.
 * toggling [linkAccount] between sign-in and sign-up modes) updates the
 * existing state, and [SignInState.launch] uses whatever is current when
 * the user taps.
 *
 * ```
 * //Github Sign-In with Custom Button and authentication with Firebase
 * val githubSignIn = rememberFirebaseGithubSignInState(onResult = onFirebaseResult)
 *
 * Button(onClick = { githubSignIn.launch() }) { Text("Github Sign-In (Custom Design)") }
 * ```
 *
 * @param requestScopes Request Scopes that is provided in Github OAuth. By Default, user's email is requested.
 * @param customParameters Custom Parameters that is provided in Github OAuth.
 * @param linkAccount [Boolean] flag to link account with current user. Default value is false.
 * @param onResult receives the signed-in [KMPAuthUser] or the failure. The
 * native Firebase user stays reachable through [KMPAuthUser.raw].
 */
@Composable
public fun rememberFirebaseGithubSignInState(
    requestScopes: List<String> = listOf("user:email"),
    customParameters: Map<String, String> = emptyMap(),
    linkAccount: Boolean = false,
    onResult: (Result<KMPAuthUser?>) -> Unit,
): SignInState = rememberFirebaseOAuthSignInState(
    provider = "github.com",
    requestScopes = requestScopes,
    customParameters = customParameters,
    linkAccount = linkAccount,
    onResult = onResult,
)
