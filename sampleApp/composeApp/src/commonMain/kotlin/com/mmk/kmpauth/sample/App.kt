package com.mmk.kmpauth.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.Divider
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
import androidx.compose.ui.unit.sp
import com.mmk.kmpauth.firebase.apple.AppleButtonUiContainer
import com.mmk.kmpauth.firebase.github.GithubButtonUiContainer
import com.mmk.kmpauth.firebase.google.GoogleButtonUiContainerFirebase
import com.mmk.kmpauth.google.GoogleButtonUiContainer
import com.mmk.kmpauth.uihelper.apple.AppleSignInButton
import com.mmk.kmpauth.uihelper.apple.AppleSignInButtonIconOnly
import com.mmk.kmpauth.uihelper.google.GoogleSignInButton
import com.mmk.kmpauth.uihelper.google.GoogleSignInButtonIconOnly
import dev.gitlive.firebase.auth.FirebaseUser

@Composable
fun App() {

    MaterialTheme {
        Column(
            Modifier.fillMaxSize().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically)
        ) {

            var signedInUserName: String by remember { mutableStateOf("") }
            val onFirebaseResult: (Result<FirebaseUser?>) -> Unit = { result ->
                if (result.isSuccess) {
                    val firebaseUser = result.getOrNull()
                    signedInUserName =
                        firebaseUser?.displayName ?: firebaseUser?.email ?: "Null User"
                } else {
                    signedInUserName = "Null User"
                    println("Error Result: ${result.exceptionOrNull()?.message}")
                }

            }
            Text(
                text = signedInUserName,
                style = MaterialTheme.typography.body1,
                textAlign = TextAlign.Start,
            )

            //Google Sign-In with Custom Button and authentication without Firebase
            GoogleButtonUiContainer(onGoogleSignInResult = { googleUser ->
                val idToken = googleUser?.idToken // Send this idToken to your backend to verify
                signedInUserName = googleUser?.displayName ?: "Null User"
            }) {
                Button(onClick = { this.onClick() }) { Text("Google Sign-In(Custom Design)") }
            }

            //Apple Sign-In with Custom Button and authentication with Firebase
            AppleButtonUiContainer(onResult = onFirebaseResult, linkAccount = false) {
                Button(onClick = { this.onClick() }) { Text("Apple Sign-In (Custom Design)") }
            }

            //Github Sign-In with Custom Button and authentication with Firebase
            GithubButtonUiContainer(onResult = onFirebaseResult, linkAccount = false) {
                Button(onClick = { this.onClick() }) { Text("Github Sign-In (Custom Design)") }
            }


            // ************************** UiHelper Text Buttons *************
            Divider(modifier = Modifier.fillMaxWidth().padding(16.dp))
            AuthUiHelperButtonsAndFirebaseAuth(
                modifier = Modifier.width(IntrinsicSize.Max),
                onFirebaseResult = onFirebaseResult
            )

            //************************** UiHelper IconOnly Buttons *************
            Divider(modifier = Modifier.fillMaxWidth().padding(16.dp))
            IconOnlyButtonsAndFirebaseAuth(
                modifier = Modifier.fillMaxWidth(),
                onFirebaseResult = onFirebaseResult
            )

        }
    }
}

@Composable
fun AuthUiHelperButtonsAndFirebaseAuth(
    modifier: Modifier = Modifier,
    onFirebaseResult: (Result<FirebaseUser?>) -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        //Google Sign-In Button and authentication with Firebase
        GoogleButtonUiContainerFirebase(onResult = onFirebaseResult, linkAccount = false) {
            GoogleSignInButton(modifier = Modifier.fillMaxWidth().height(44.dp), fontSize = 19.sp) { this.onClick() }
        }

        //Apple Sign-In Button and authentication with Firebase
        AppleButtonUiContainer(onResult = onFirebaseResult, linkAccount = false) {
            AppleSignInButton(modifier = Modifier.fillMaxWidth().height(44.dp)) { this.onClick() }
        }

    }
}

@Composable
fun IconOnlyButtonsAndFirebaseAuth(
    modifier: Modifier = Modifier,
    onFirebaseResult: (Result<FirebaseUser?>) -> Unit,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
    ) {

        //Google Sign-In IconOnly Button and authentication with Firebase
        GoogleButtonUiContainerFirebase(onResult = onFirebaseResult, linkAccount = false) {
            GoogleSignInButtonIconOnly(onClick = { this.onClick() })
        }

        //Apple Sign-In IconOnly Button and authentication with Firebase
        AppleButtonUiContainer(onResult = onFirebaseResult, linkAccount = false) {
            AppleSignInButtonIconOnly(onClick = { this.onClick() })
        }
    }
}