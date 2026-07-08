package com.mmk.kmpauth.facebook

import androidx.compose.runtime.Composable
import com.mmk.kmpauth.core.SignInState

/**
 * Facebook Sign-In (without any backend) as a Compose state holder.
 *
 * Parameters are read at launch time: recomposing with new values updates
 * the existing state, and [SignInState.launch] uses whatever is current
 * when the user taps.
 *
 * ```
 * val facebookSignIn = rememberFacebookSignInState(onResult = { result ->
 *     val accessToken = result.getOrNull()?.accessToken // send to your backend
 * })
 *
 * Button(onClick = { facebookSignIn.launch() }) { Text("Facebook Sign-In (Custom Design)") }
 * ```
 *
 * @param requestScopes Request Scopes that is provided in Facebook OAuth. By Default, user's email
 * and public profile info is requested.
 * @param linkAccount [Boolean] flag to link account with current user. Default value is false.
 * @param loginTracking [FacebookLoginTracking] mode controlling which token is
 * returned. Defaults to [FacebookLoginTracking.Limited] (privacy-friendly OIDC
 * JWT + nonce); use [FacebookLoginTracking.Enabled] to receive a Graph-API
 * access token. See [FacebookLoginTracking] for the trade-offs.
 * @param onResult receives the signed-in [FacebookUser] or the failure.
 */
@Composable
public expect fun rememberFacebookSignInState(
    requestScopes: List<FacebookSignInRequestScope> = listOf(
        FacebookSignInRequestScope.PublicProfile,
        FacebookSignInRequestScope.Email
    ),
    linkAccount: Boolean = false,
    loginTracking: FacebookLoginTracking = FacebookLoginTracking.Limited,
    onResult: (Result<FacebookUser>) -> Unit,
): SignInState
