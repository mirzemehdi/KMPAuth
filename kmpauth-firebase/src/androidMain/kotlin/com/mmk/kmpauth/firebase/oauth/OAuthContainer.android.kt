package com.mmk.kmpauth.firebase.oauth

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.tasks.Task
import com.mmk.kmpauth.core.KMPAuthInternalApi
import com.mmk.kmpauth.core.UiContainerScope
import com.mmk.kmpauth.core.getActivity
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.OAuthProvider
import dev.gitlive.firebase.auth.auth

@OptIn(KMPAuthInternalApi::class)
@Composable
public actual fun OAuthContainer(
    modifier: Modifier,
    oAuthProvider: OAuthProvider,
    onResult: (Result<FirebaseUser?>) -> Unit,
    content: @Composable UiContainerScope.() -> Unit,
) {
    val activity = LocalContext.current.getActivity()
    val uiContainerScope = remember {
        object : UiContainerScope {
            override fun onClick() {
                onClickSignIn(activity, oAuthProvider, onResult)
            }
        }
    }
    Box(modifier = modifier) { uiContainerScope.content() }
}

private fun onClickSignIn(
    activity: ComponentActivity?,
    oAuthProvider: OAuthProvider,
    onResult: (Result<FirebaseUser?>) -> Unit,
) {
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


