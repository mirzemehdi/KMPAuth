package com.mmk.kmpauth.firebase.facebook

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import com.mmk.kmpauth.core.KMPAuthInternalApi
import com.mmk.kmpauth.core.LaunchingSignInState
import com.mmk.kmpauth.core.SignInState
import com.mmk.kmpauth.core.logger.currentLogger
import com.mmk.kmpauth.facebook.FacebookSignInRequestScope
import com.mmk.kmpauth.facebook.FacebookUser
import com.mmk.kmpauth.facebook.rememberFacebookSignInState
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.OAuthProvider
import dev.gitlive.firebase.auth.auth
import kotlinx.coroutines.CompletableDeferred
import kotlin.coroutines.cancellation.CancellationException

/**
 * Bridges the callback-based [rememberFacebookSignInState] result into the
 * suspend block of the Firebase sign-in state, so [SignInState.isInProgress]
 * stays true until the Firebase exchange completes.
 */
private class PendingFacebookResult {
    var deferred: CompletableDeferred<Result<FacebookUser>>? = null
}

@OptIn(KMPAuthInternalApi::class)
@Composable
public actual fun rememberFirebaseFacebookSignInState(
    requestScopes: List<FacebookSignInRequestScope>,
    linkAccount: Boolean,
    onResult: (Result<FirebaseUser?>) -> Unit,
): SignInState {
    val scope = rememberCoroutineScope()
    val currentLinkAccount by rememberUpdatedState(linkAccount)
    val currentOnResult by rememberUpdatedState(onResult)

    val pendingFacebookResult = remember { PendingFacebookResult() }
    val facebookSignInState = rememberFacebookSignInState(
        requestScopes = requestScopes,
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
                        currentLogger.log("Facebook Login successful, attempting to sign in with Firebase")
                        val accessToken = facebookUser.accessToken
                        if (accessToken == null) {
                            currentLogger.log("Facebook accessToken is null")
                            currentOnResult(Result.failure(IllegalStateException("Facebook accessToken is null")))
                            return@onSuccess
                        }

                        try {
                            val credential = OAuthProvider.credential(
                                providerId = "facebook.com",
                                idToken = accessToken,
                                rawNonce = facebookUser.nonce
                            )

                            val auth = Firebase.auth
                            val currentUser = auth.currentUser


                            val firebaseAuthResult = if (currentLinkAccount && currentUser != null) {
                                currentLogger.log("Linking Facebook account with current firebase user: ${currentUser.uid}")
                                currentUser.linkWithCredential(credential)
                            } else {
                                currentLogger.log("Signing in with Facebook account on Firebase")
                                auth.signInWithCredential(credential)
                            }

                            val user = firebaseAuthResult.user
                            if (user == null) {
                                currentLogger.log("Firebase sign-in failed: Firebase user is null")
                                currentOnResult(Result.failure(IllegalStateException("Firebase user is null")))
                            } else {
                                currentLogger.log("Firebase sign-in successful")
                                currentOnResult(Result.success(user))
                            }

                        } catch (e: Exception) {
                            if (e is CancellationException) throw e
                            currentLogger.log("Firebase sign-in failed with error: ${e.message}")
                            currentOnResult(Result.failure(e))
                        }
                    }
            } finally {
                pendingFacebookResult.deferred = null
            }
        }
    }
}
