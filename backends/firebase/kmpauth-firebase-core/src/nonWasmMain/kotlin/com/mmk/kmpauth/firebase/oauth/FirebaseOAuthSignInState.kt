package com.mmk.kmpauth.firebase.oauth

import androidx.compose.runtime.Composable
import com.mmk.kmpauth.core.SignInState
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.OAuthProvider

/**
 * OAuth sign-in with Firebase for the given provider, as a Compose state
 * holder.
 *
 * Parameters are read at launch time: recomposing with new values (e.g.
 * toggling [linkAccount] between sign-in and sign-up modes) updates the
 * existing state, and [SignInState.launch] uses whatever is current when
 * the user taps.
 *
 * ```
 * val oAuthProvider = OAuthProvider(provider = "github.com")
 * val oAuthSignIn = rememberFirebaseOAuthSignInState(
 *     oAuthProvider = oAuthProvider,
 *     onResult = onFirebaseResult,
 * )
 *
 * Button(onClick = { oAuthSignIn.launch() }) { Text("Github Sign-In (Custom Design)") }
 * ```
 *
 * @param oAuthProvider [OAuthProvider] class object.
 * @param linkAccount [Boolean] flag to link account with current user. Default value is false.
 * @param onResult receives the signed-in [FirebaseUser] or the failure.
 */
@Composable
public expect fun rememberFirebaseOAuthSignInState(
    oAuthProvider: OAuthProvider,
    linkAccount: Boolean = false,
    onResult: (Result<FirebaseUser?>) -> Unit,
): SignInState
