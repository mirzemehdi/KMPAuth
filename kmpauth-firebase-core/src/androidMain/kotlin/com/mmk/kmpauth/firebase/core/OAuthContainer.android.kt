package com.mmk.kmpauth.firebase.core

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.tasks.Task
import com.mmk.kmpauth.core.getActivity
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.OAuthProvider
import dev.gitlive.firebase.auth.auth

@Composable
internal actual fun OAuthContainer(
    oAuthProvider: OAuthProvider,
    onResult: (Result<FirebaseUser?>) -> Unit,
) {
    val activity = LocalContext.current.getActivity()
    val auth = Firebase.auth.android
    val pendingAuthResult = auth.pendingAuthResult
    if (pendingAuthResult != null) {
        pendingAuthResult.resultAsFirebaseUser(onResult)
    } else {
        if (activity == null)
            onResult(Result.failure(IllegalStateException("Activity is null")))
        else
            auth.startActivityForSignInWithProvider(activity, oAuthProvider.android)
                .resultAsFirebaseUser(onResult)
    }
}

private fun <T> Task<T>.resultAsFirebaseUser(onResult: (Result<FirebaseUser?>) -> Unit) {
    this
        .addOnSuccessListener {
            onResult(Result.success(Firebase.auth.currentUser))
        }.addOnFailureListener {
            onResult(Result.failure(it))
        }
}


