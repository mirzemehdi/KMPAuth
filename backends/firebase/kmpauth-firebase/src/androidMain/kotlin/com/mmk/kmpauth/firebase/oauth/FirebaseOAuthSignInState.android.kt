package com.mmk.kmpauth.firebase.oauth

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.tasks.Task
import com.mmk.kmpauth.core.KMPAuthInternalApi
import com.mmk.kmpauth.core.LaunchingSignInState
import com.mmk.kmpauth.core.SignInState
import com.mmk.kmpauth.core.getActivity
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.OAuthProvider
import dev.gitlive.firebase.auth.android
import dev.gitlive.firebase.auth.auth
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@OptIn(KMPAuthInternalApi::class)
@Composable
internal actual fun rememberFirebaseGitLiveOAuthSignInState(
    oAuthProvider: OAuthProvider,
    linkAccount: Boolean,
    onResult: (Result<FirebaseUser?>) -> Unit,
): SignInState {
    val activity = LocalContext.current.getActivity()
    val scope = rememberCoroutineScope()
    val currentOAuthProvider by rememberUpdatedState(oAuthProvider)
    val currentLinkAccount by rememberUpdatedState(linkAccount)
    val currentOnResult by rememberUpdatedState(onResult)

    return remember {
        LaunchingSignInState(scope) {
            currentOnResult(onClickSignIn(activity, currentOAuthProvider, currentLinkAccount))
        }
    }
}

private suspend fun onClickSignIn(
    activity: ComponentActivity?,
    oAuthProvider: OAuthProvider,
    linkAccount: Boolean,
): Result<FirebaseUser?> {
    val auth = Firebase.auth.android
    val pendingAuthResult = auth.pendingAuthResult
    return if (pendingAuthResult != null) {
        pendingAuthResult.resultAsFirebaseUser()
    } else {
        if (activity == null)
            Result.failure(IllegalStateException("Activity is null"))
        else {
            val currentUser = auth.currentUser
            val result = if (linkAccount && currentUser != null) {
                currentUser.startActivityForLinkWithProvider(activity, oAuthProvider.android)
            } else {
                auth.startActivityForSignInWithProvider(activity, oAuthProvider.android)
            }

            result.resultAsFirebaseUser()
        }
    }
}

private suspend fun <T> Task<T>.resultAsFirebaseUser(): Result<FirebaseUser?> =
    suspendCoroutine { continuation ->
        this
            .addOnSuccessListener {
                continuation.resume(Result.success(Firebase.auth.currentUser))
            }.addOnFailureListener {
                continuation.resume(Result.failure(it))
            }
    }
