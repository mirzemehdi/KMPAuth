package com.mmk.kmpauth.firebase.oauth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import cocoapods.FirebaseAuth.FIRAuthCredential
import cocoapods.FirebaseAuth.FIRAuthDataResult
import com.mmk.kmpauth.core.KMPAuthInternalApi
import com.mmk.kmpauth.core.LaunchingSignInState
import com.mmk.kmpauth.core.SignInState
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.AuthCredential
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.OAuthProvider
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.auth.ios
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSError
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

//On iOS this is needed for some reason, app is recomposed again when navigate to OAuth Screen.
// rememberUpdatedState doesn't solve the problem
private var mOnResult: ((Result<FirebaseUser?>) -> Unit)? = null

@OptIn(KMPAuthInternalApi::class)
@Composable
internal actual fun rememberFirebaseGitLiveOAuthSignInState(
    oAuthProvider: OAuthProvider,
    linkAccount: Boolean,
    onResult: (Result<FirebaseUser?>) -> Unit,
): SignInState {
    val scope = rememberCoroutineScope()
    val currentOAuthProvider by rememberUpdatedState(oAuthProvider)
    val currentLinkAccount by rememberUpdatedState(linkAccount)
    val updatedOnResultFunc by rememberUpdatedState(onResult)
    mOnResult = updatedOnResultFunc

    return remember {
        LaunchingSignInState(scope) {
            val result = onClickSignIn(currentOAuthProvider, currentLinkAccount)
            mOnResult?.invoke(result)
            mOnResult = null
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private suspend fun onClickSignIn(
    oAuthProvider: OAuthProvider,
    linkAccount: Boolean,
): Result<FirebaseUser?> = suspendCoroutine { continuation ->
    oAuthProvider.ios.getCredentialWithUIDelegate(null,
        completion = { firAuthCredential, nsError ->
            if (firAuthCredential != null) {
                val authCredential = firAuthCredential.asAuthCredential().ios
                val auth = Firebase.auth.ios
                val currentUser = auth.currentUser()

                val handleResult: (FIRAuthDataResult?, NSError?) -> Unit = { result, linkError ->
                    if (result != null) continuation.resume(Result.success(Firebase.auth.currentUser))
                    else continuation.resume(Result.failure(IllegalStateException(linkError?.localizedFailureReason)))
                }

                if (linkAccount && currentUser != null) {
                    currentUser.linkWithCredential(authCredential, handleResult)
                } else {
                    auth.signInWithCredential(authCredential, handleResult)
                }
            } else
                continuation.resume(Result.failure(IllegalStateException(nsError?.localizedFailureReason)))

        })
}

@OptIn(ExperimentalForeignApi::class)
private fun FIRAuthCredential.asAuthCredential(): AuthCredential = object : AuthCredential(this) {}
