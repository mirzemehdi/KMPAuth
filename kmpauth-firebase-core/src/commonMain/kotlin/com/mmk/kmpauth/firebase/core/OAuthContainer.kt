package com.mmk.kmpauth.firebase.core

import androidx.compose.runtime.Composable
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.OAuthProvider

@Composable internal expect fun OAuthContainer(
    oAuthProvider: OAuthProvider,
    onResult: (Result<FirebaseUser?>) -> Unit
)