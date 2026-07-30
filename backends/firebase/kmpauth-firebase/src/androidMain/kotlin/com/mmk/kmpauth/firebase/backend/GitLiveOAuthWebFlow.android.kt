package com.mmk.kmpauth.firebase.backend

import com.google.android.gms.tasks.Task
import com.mmk.kmpauth.core.KMPAuthInternalApi
import com.mmk.kmpauth.core.auth.AndroidActivityTracker
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.OAuthProvider
import dev.gitlive.firebase.auth.android
import dev.gitlive.firebase.auth.auth
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

@OptIn(KMPAuthInternalApi::class)
internal actual suspend fun gitLiveOAuthWebFlowSignIn(
    oAuthProvider: OAuthProvider,
    linkAccount: Boolean,
): FirebaseUser? {
    val auth = Firebase.auth.android
    val pendingAuthResult = auth.pendingAuthResult
    if (pendingAuthResult != null) return pendingAuthResult.awaitFirebaseUser()

    // The Firebase SDK needs an Activity to launch its Custom Tab web flow.
    val activity = AndroidActivityTracker.currentActivity
        ?: throw IllegalStateException("OAuth web-flow sign-in requires a foreground Activity")
    val currentUser = auth.currentUser
    val task = if (linkAccount && currentUser != null) {
        currentUser.startActivityForLinkWithProvider(activity, oAuthProvider.android)
    } else {
        auth.startActivityForSignInWithProvider(activity, oAuthProvider.android)
    }
    return task.awaitFirebaseUser()
}

private suspend fun <T> Task<T>.awaitFirebaseUser(): FirebaseUser? =
    suspendCoroutine { continuation ->
        addOnSuccessListener { continuation.resume(Firebase.auth.currentUser) }
        addOnFailureListener { continuation.resumeWithException(it) }
    }
