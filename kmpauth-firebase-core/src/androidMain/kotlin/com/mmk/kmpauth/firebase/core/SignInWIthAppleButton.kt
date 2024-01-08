package com.mmk.kmpauth.firebase.core

import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.OAuthProvider

@Composable
public actual fun SignInWithAppleButton(
    modifier: Modifier,
    onResult: (Result<FirebaseUser?>) -> Unit,
) {
    val oAuthProvider = OAuthProvider(provider = "apple.com", scopes = listOf("email,name"))
    OAuthContainer(
        oAuthProvider = oAuthProvider,
        onResult = { onResult(it) }
    ) {
        Button(onClick = { this.onClick() }) {
            Text("Sign-In with Apple (Firebase)")
        }
    }
}