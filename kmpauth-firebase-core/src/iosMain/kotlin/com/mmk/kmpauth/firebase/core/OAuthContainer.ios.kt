package com.mmk.kmpauth.firebase.core

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import cocoapods.FirebaseAuth.FIRAuthCredential
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.AuthCredential
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.OAuthProvider
import dev.gitlive.firebase.auth.auth
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Composable
public actual fun OAuthContainer(
    modifier: Modifier,
    oAuthProvider: OAuthProvider,
    onResult: (Result<FirebaseUser?>) -> Unit,
    content: @Composable UiContainerScope.() -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val uiContainerScope = remember {
        object : UiContainerScope {
            override fun onClick() {
                coroutineScope.launch {
                    val result = onClickSignIn(oAuthProvider)
                    onResult(result)
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
