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
import com.mmk.kmpauth.google.GoogleButtonUiContainer
import com.mmk.kmpauth.google.GoogleUser

@Composable
fun App() {

    MaterialTheme {
        Column(
            Modifier.fillMaxSize().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            var signedInUser:GoogleUser? by remember { mutableStateOf(null) }
            GoogleButtonUiContainer(onGoogleSignInResult = { googleUser ->
                val idToken=googleUser?.idToken // Send this idToken to your backend to verify
                signedInUser=googleUser
            }) {
                Button(
                    onClick = { this.onClick() }
                ) {
                    Text("Sign-In with Google")
                }
            }

            Text(
                modifier = Modifier.padding(20.dp),
                text = "Hello World, ${signedInUser?.displayName?:""}",
                style = MaterialTheme.typography.body1,
                textAlign = TextAlign.Start,
            )
        }
    }
}