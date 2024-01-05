package com.mmk.kmpauth.firebase.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import cocoapods.FirebaseAuth.FIRAuthCredential
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.AuthCredential
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.OAuthProvider
import dev.gitlive.firebase.auth.auth
import kotlinx.coroutines.launch

@Composable
internal actual fun OAuthContainer(
    oAuthProvider: OAuthProvider,
    onResult: (Result<FirebaseUser?>) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val auth = Firebase.auth
    oAuthProvider.ios.getCredentialWithUIDelegate(null, completion = { firAuthCredential, nsError ->
        if (firAuthCredential != null) {
            val authCredential = firAuthCredential.asAuthCredential()
            coroutineScope.launch {
                val authResult = auth.signInWithCredential(authCredential)
                onResult(Result.success(authResult.user))
            }
            return@getCredentialWithUIDelegate
        }

        if (nsError != null) {
            onResult(Result.failure(IllegalStateException(nsError.localizedFailureReason)))
        }

    })
}

private fun FIRAuthCredential.asAuthCredential(): AuthCredential = object : AuthCredential(this) {}
