package com.mmk.kmpauth.firebase.backend

import cocoapods.FirebaseAuth.FIRAuthCredential
import cocoapods.FirebaseAuth.FIRAuthDataResult
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.AuthCredential
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.OAuthProvider
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.auth.ios
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSError
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

@OptIn(ExperimentalForeignApi::class)
internal actual suspend fun gitLiveOAuthWebFlowSignIn(
    oAuthProvider: OAuthProvider,
    linkAccount: Boolean,
): FirebaseUser? = suspendCoroutine { continuation ->
    oAuthProvider.ios.getCredentialWithUIDelegate(
        null,
        completion = { firAuthCredential, nsError ->
            if (firAuthCredential != null) {
                val authCredential = firAuthCredential.asAuthCredential().ios
                val auth = Firebase.auth.ios
                val currentUser = auth.currentUser()

                val handleResult: (FIRAuthDataResult?, NSError?) -> Unit = { result, error ->
                    if (result != null) continuation.resume(Firebase.auth.currentUser)
                    else continuation.resumeWithException(
                        IllegalStateException(
                            error?.localizedFailureReason ?: error?.localizedDescription
                        )
                    )
                }

                if (linkAccount && currentUser != null) {
                    currentUser.linkWithCredential(authCredential, handleResult)
                } else {
                    auth.signInWithCredential(authCredential, handleResult)
                }
            } else continuation.resumeWithException(
                IllegalStateException(
                    nsError?.localizedFailureReason ?: nsError?.localizedDescription
                )
            )
        },
    )
}

@OptIn(ExperimentalForeignApi::class)
private fun FIRAuthCredential.asAuthCredential(): AuthCredential = object : AuthCredential(this) {}
