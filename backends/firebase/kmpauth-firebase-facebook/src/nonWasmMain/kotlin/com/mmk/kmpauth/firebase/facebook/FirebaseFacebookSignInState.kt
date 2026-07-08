package com.mmk.kmpauth.firebase.facebook

import androidx.compose.runtime.Composable
import com.mmk.kmpauth.core.SignInState
import com.mmk.kmpauth.facebook.FacebookLoginTracking
import com.mmk.kmpauth.facebook.FacebookSignInRequestScope
import dev.gitlive.firebase.auth.FirebaseUser

/**
 * Facebook Sign-In exchanged for a Firebase session, as a Compose state
 * holder.
 *
 * Parameters are read at launch time: recomposing with new values (e.g.
 * toggling [linkAccount] between sign-in and sign-up modes) updates the
 * existing state, and [SignInState.launch] uses whatever is current when
 * the user taps.
 *
 * ```
 * //Facebook Sign-In with Custom Button and authentication with Firebase
 * val facebookSignIn = rememberFirebaseFacebookSignInState(onResult = onFirebaseResult)
 *
 * Button(onClick = { facebookSignIn.launch() }) { Text("Facebook Sign-In (Custom Design)") }
 * ```
 *
 * @param requestScopes Request Scopes that is provided in Facebook OAuth. By Default, user's email
 * and public profile info is requested.
 * @param linkAccount [Boolean] flag to link account with current user. Default value is false.
 * @param loginTracking [FacebookLoginTracking] mode. Defaults to
 * [FacebookLoginTracking.Limited], exchanged with Firebase through the OIDC
 * OAuth provider; [FacebookLoginTracking.Enabled] uses a classic access token
 * via Firebase's `FacebookAuthProvider`. See [FacebookLoginTracking].
 * @param onResult receives the signed-in [FirebaseUser] or the failure.
 */
@Composable
public expect fun rememberFirebaseFacebookSignInState(
    requestScopes: List<FacebookSignInRequestScope> = listOf(
        FacebookSignInRequestScope.PublicProfile,
        FacebookSignInRequestScope.Email
    ),
    linkAccount: Boolean = false,
    loginTracking: FacebookLoginTracking = FacebookLoginTracking.Limited,
    onResult: (Result<FirebaseUser?>) -> Unit,
): SignInState
