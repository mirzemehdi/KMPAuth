package com.mmk.kmpauth.firebase.facebook

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import com.mmk.kmpauth.core.KMPAuthInternalApi
import com.mmk.kmpauth.core.UiContainerScope
import com.mmk.kmpauth.core.logger.currentLogger
import com.mmk.kmpauth.facebook.FacebookButtonUiContainer
import com.mmk.kmpauth.facebook.FacebookSignInRequestScope
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.FacebookAuthProvider
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.auth
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException


@OptIn(KMPAuthInternalApi::class)
@Composable
public actual fun FacebookButtonUiContainerFirebase(
    modifier: Modifier,
    requestScopes: List<FacebookSignInRequestScope>,
    onResult: (Result<FirebaseUser?>) -> Unit,
    linkAccount: Boolean,
    content: @Composable (UiContainerScope.() -> Unit)
) {
    val updatedOnResult by rememberUpdatedState(onResult)
    val coroutineScope = rememberCoroutineScope()
    FacebookButtonUiContainer(
        modifier = modifier,

        requestScopes = requestScopes,
        onResult = { facebookUserResult ->

            facebookUserResult
                .onFailure { error ->
                    updatedOnResult(Result.failure(error))
                }.onSuccess { facebookUser ->
                    currentLogger.log("Facebook Login successful, attempting to sign in with Firebase")
                    val accessToken = facebookUser.accessToken
                    if (accessToken == null) {
                        currentLogger.log("Facebook accessToken is null")
                        updatedOnResult(Result.failure(IllegalStateException("Facebook accessToken is null")))
                        return@FacebookButtonUiContainer
                    }

                    val authCredential = FacebookAuthProvider.credential(accessToken)
                    coroutineScope.launch {
                        try {
                            val auth = Firebase.auth
                            val currentUser = auth.currentUser
                            val firebaseAuthResult = if (linkAccount && currentUser != null) {
                                currentLogger.log("Linking Facebook account with current firebase user: ${currentUser.uid}")
                                currentUser.linkWithCredential(authCredential)
                            } else {
                                currentLogger.log("Signing in with Facebook account on Firebase")
                                auth.signInWithCredential(authCredential)
                            }
                            val user = firebaseAuthResult.user
                            if (user == null) {
                                currentLogger.log("Firebase sign-in failed: Firebase user is null")
                                updatedOnResult(Result.failure(IllegalStateException("Firebase user is null")))
                            } else {
                                currentLogger.log("Firebase sign-in successful")
                                updatedOnResult(Result.success(user))
                            }
                        } catch (e: Exception) {
                            if (e is CancellationException) throw e

                            currentLogger.log("Firebase sign-in failed with error: ${e.message}")
                            updatedOnResult(Result.failure(e))
                        }
                    }
                }

        },
        content = content
    )
}