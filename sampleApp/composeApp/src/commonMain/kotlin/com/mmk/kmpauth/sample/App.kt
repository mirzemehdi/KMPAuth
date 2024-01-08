package com.mmk.kmpauth.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mmk.kmpauth.firebase.core.OAuthContainer
import com.mmk.kmpauth.firebase.core.SignInWithAppleButton
import com.mmk.kmpauth.google.GoogleButtonUiContainer
import com.mmk.kmpauth.google.GoogleUser
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.OAuthProvider

@Composable
fun App() {

    MaterialTheme {
        Column(
            Modifier.fillMaxSize().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically)
        ) {

            var signedInUserName: String by remember { mutableStateOf("") }
            Text(
                text = signedInUserName,
                style = MaterialTheme.typography.body1,
                textAlign = TextAlign.Start,
            )
            GoogleSignInWithoutFirebase { googleUser ->
                signedInUserName = googleUser?.displayName ?: ""
            }

            GithubSignInWithFirebase { firebaseUser ->
                println("Firebase:${firebaseUser?.displayName}")
                signedInUserName = firebaseUser?.displayName ?: ""
            }

            SignInWithAppleButton { result ->
                val firebaseUser = result.getOrNull()
                println("Firebase:${firebaseUser?.email}")
                signedInUserName = firebaseUser?.email ?: ""
            }

        }
    }
}

@Composable
fun GoogleSignInWithoutFirebase(onSignedInGoogleUser: (GoogleUser?) -> Unit) {
    GoogleButtonUiContainer(onGoogleSignInResult = { googleUser ->
        val idToken = googleUser?.idToken // Send this idToken to your backend to verify
        onSignedInGoogleUser(googleUser)
    }) {
        Button(
            onClick = { this.onClick() }
        ) {
            Text("Sign-In with Google (without Firebase)")
        }
    }
}

@Composable
fun GithubSignInWithFirebase(onSignedInFirebaseUser: (FirebaseUser?) -> Unit) {
    val oAuthProvider = OAuthProvider(provider = "github.com", scopes = listOf("user:email"))
    OAuthContainer(
        oAuthProvider = oAuthProvider,
        onResult = { result ->
            if (result.isSuccess) onSignedInFirebaseUser(result.getOrNull())
            else println(result.exceptionOrNull()?.message)
        }
    ) {
        Button(
            onClick = { this.onClick() }
        ) {
            Text("Sign-In with Github (Firebase)")
        }
    }


}