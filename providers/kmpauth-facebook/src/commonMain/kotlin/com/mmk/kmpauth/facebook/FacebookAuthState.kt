package com.mmk.kmpauth.facebook

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import com.mmk.kmpauth.core.KMPAuthInternalApi
import com.mmk.kmpauth.core.LaunchingSignInState
import com.mmk.kmpauth.core.SignInState
import com.mmk.kmpauth.core.auth.AuthCredential
import com.mmk.kmpauth.core.auth.AuthProviderIds
import com.mmk.kmpauth.core.auth.KMPAuthBackend
import com.mmk.kmpauth.core.auth.KMPAuthUser
import com.mmk.kmpauth.core.logger.currentLogger
import kotlinx.coroutines.CompletableDeferred

/**
 * Bridges the callback-based [rememberFacebookSignInState] result into the
 * suspend block of the auth state, so [SignInState.isInProgress] stays true
 * until the backend exchange completes.
 */
private class PendingFacebookResult {
    var deferred: CompletableDeferred<Result<FacebookUser>>? = null
}

/**
 * Facebook Login exchanged for a session by the registered auth backend
 * (Firebase today; any
 * [AuthProviderBackend][com.mmk.kmpauth.core.auth.AuthProviderBackend]),
 * as a Compose state holder. For the Facebook credential alone — no
 * backend session — use [rememberFacebookSignInState].
 *
 * With `kmpauth-firebase-core` in the dependencies the Firebase backend
 * registers itself automatically; a custom backend is registered via
 * `KMPAuth.registerBackendProvider`.
 *
 * The Facebook login token is exchanged according to [loginTracking]:
 * [FacebookLoginTracking.Limited] yields an OIDC token (JWT + nonce),
 * [FacebookLoginTracking.Enabled] a classic access token. The token itself
 * is carried in [FacebookUser.accessToken] in both cases (see
 * [FacebookLoginTracking]).
 *
 * ```
 * val facebookAuth = rememberFacebookAuthState(onResult = onAuthResult)
 *
 * Button(onClick = { facebookAuth.launch() }) { Text("Facebook Sign-In") }
 * ```
 *
 * @param requestScopes Request Scopes that is provided in Facebook OAuth. By Default, user's email
 * and public profile info is requested.
 * @param linkAccount [Boolean] flag to link account with current user. Default value is false.
 * @param loginTracking [FacebookLoginTracking] mode. Defaults to
 * [FacebookLoginTracking.Limited]. See [FacebookLoginTracking].
 * @param onResult receives the signed-in [KMPAuthUser] or the failure. The
 * backend's native user stays reachable through [KMPAuthUser.raw].
 */
@OptIn(KMPAuthInternalApi::class)
@Composable
public fun rememberFacebookAuthState(
    requestScopes: List<FacebookSignInRequestScope> = listOf(
        FacebookSignInRequestScope.PublicProfile,
        FacebookSignInRequestScope.Email
    ),
    linkAccount: Boolean = false,
    loginTracking: FacebookLoginTracking = FacebookLoginTracking.Limited,
    onResult: (Result<KMPAuthUser>) -> Unit,
): SignInState {
    val scope = rememberCoroutineScope()
    val currentLinkAccount by rememberUpdatedState(linkAccount)
    val currentLoginTracking by rememberUpdatedState(loginTracking)
    val currentOnResult by rememberUpdatedState(onResult)

    val pendingFacebookResult = remember { PendingFacebookResult() }
    val facebookSignInState = rememberFacebookSignInState(
        requestScopes = requestScopes,
        loginTracking = loginTracking,
        onResult = { facebookUserResult ->
            pendingFacebookResult.deferred?.complete(facebookUserResult)
        },
    )

    return remember {
        LaunchingSignInState(scope) {
            val facebookUserDeferred = CompletableDeferred<Result<FacebookUser>>()
            pendingFacebookResult.deferred = facebookUserDeferred
            try {
                facebookSignInState.launch()
                val facebookUserResult = facebookUserDeferred.await()

                facebookUserResult
                    .onFailure { error ->
                        currentOnResult(Result.failure(error))
                    }.onSuccess { facebookUser ->
                        currentLogger.log("Facebook Login successful, exchanging with the auth backend")
                        val token = facebookUser.accessToken
                        if (token == null) {
                            currentLogger.log("Facebook token is null")
                            currentOnResult(Result.failure(IllegalStateException("Facebook token is null")))
                            return@onSuccess
                        }

                        val credential = when (currentLoginTracking) {
                            // Limited Login issues an OIDC JWT; the nonce lets
                            // the backend verify it.
                            FacebookLoginTracking.Limited -> AuthCredential.IdToken(
                                providerId = AuthProviderIds.FACEBOOK,
                                idToken = token,
                                rawNonce = facebookUser.nonce,
                            )

                            FacebookLoginTracking.Enabled -> AuthCredential.IdToken(
                                providerId = AuthProviderIds.FACEBOOK,
                                idToken = token,
                                accessToken = token,
                            )
                        }

                        currentOnResult(
                            KMPAuthBackend.signIn(credential, currentLinkAccount)
                        )
                    }
            } finally {
                pendingFacebookResult.deferred = null
            }
        }
    }
}
