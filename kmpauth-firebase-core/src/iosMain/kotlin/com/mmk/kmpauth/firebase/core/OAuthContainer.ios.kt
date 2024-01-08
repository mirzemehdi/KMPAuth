package com.mmk.kmpauth.firebase.core

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import cocoapods.FirebaseAuth.FIRAuthCredential
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.AuthCredential
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.OAuthProvider
import dev.gitlive.firebase.auth.auth
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

//On iOS this is needed for some reason, app is recomposed again when navigate to OAuth Screen.
// rememberUpdatedState doesn't solve the problem
private var mOnResult: ((Result<FirebaseUser?>) -> Unit)? = null

@Composable
public actual fun OAuthContainer(
    modifier: Modifier,
    oAuthProvider: OAuthProvider,
    onResult: (Result<FirebaseUser?>) -> Unit,
    content: @Composable UiContainerScope.() -> Unit,
) {
    mOnResult = onResult
    val coroutineScope = MainScope()
    val uiContainerScope = remember {
        object : UiContainerScope {
            override fun onClick() {
                coroutineScope.launch {
                    val result = onClickSignIn(oAuthProvider)
                    mOnResult?.invoke(result)
                    mOnResult = null
                }
            }

        }
    }
    Box(modifier = modifier) { uiContainerScope.content() }
}

private suspend fun onClickSignIn(
    oAuthProvider: OAuthProvider,
): Result<FirebaseUser?> = suspendCoroutine { continuation ->
    oAuthProvider.ios.getCredentialWithUIDelegate(null,
        completion = { firAuthCredential, nsError ->
            if (firAuthCredential != null) {
                val authCredential = firAuthCredential.asAuthCredential().ios
                val auth = Firebase.auth.ios
                auth.signInWithCredential(authCredential) { result, signInError ->
                    if (result != null) continuation.resume(Result.success(Firebase.auth.currentUser))
                    else continuation.resume(Result.failure(IllegalStateException(signInError?.localizedFailureReason)))
                }
            } else
                continuation.resume(Result.failure(IllegalStateException(nsError?.localizedFailureReason)))

        })
}

private fun FIRAuthCredential.asAuthCredential(): AuthCredential = object : AuthCredential(this) {}
