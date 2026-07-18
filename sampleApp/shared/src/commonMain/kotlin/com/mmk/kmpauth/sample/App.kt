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
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.mmk.kmpauth.apple.rememberAppleSignInState
import com.mmk.kmpauth.firebase.apple.rememberFirebaseAppleSignInState
import com.mmk.kmpauth.firebase.facebook.rememberFirebaseFacebookSignInState
import com.mmk.kmpauth.firebase.github.rememberFirebaseGithubSignInState
import com.mmk.kmpauth.firebase.google.rememberFirebaseGoogleSignInState
import com.mmk.kmpauth.google.rememberGoogleSignInState
import com.mmk.kmpauth.uihelper.apple.AppleSignInButton
import com.mmk.kmpauth.uihelper.apple.AppleSignInButtonIconOnly
import com.mmk.kmpauth.uihelper.facebook.FacebookSignInButton
import com.mmk.kmpauth.uihelper.facebook.FacebookSignInButtonIconOnly
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
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Start,
            )

            //Google Sign-In with Custom Button and authentication without Firebase
            val googleSignIn = rememberGoogleSignInState(onResult = { googleUser ->
                val idToken = googleUser?.idToken // Send this idToken to your backend to verify
                signedInUserName = googleUser?.displayName ?: "Null User"
            })
            Button(onClick = { googleSignIn.launch() }) { Text("Google Sign-In(Custom Design)") }

            //Apple Sign-In with Custom Button and authentication with Firebase
            val appleSignIn = rememberFirebaseAppleSignInState(onResult = onFirebaseResult)
            Button(onClick = { appleSignIn.launch() }) { Text("Apple Sign-In (Custom Design)") }

            //Native Apple Sign-In without Firebase. Apple platforms only - on
            //Android/Desktop/Web this state is a no-op (see kmpauth-apple docs).
            val appleNativeSignIn = rememberAppleSignInState(onResult = { result ->
                val appleUser = result.getOrNull()
                // Send appleUser?.idToken (with nonce) to your backend to verify.
                // email/fullName are only returned on the first authorization.
                signedInUserName = appleUser?.fullName
                    ?: appleUser?.email
                    ?: appleUser?.userId
                    ?: "Null User"
            })
            Button(onClick = { appleNativeSignIn.launch() }) { Text("Apple Sign-In (No Firebase)") }

            //Github Sign-In with Custom Button and authentication with Firebase
            val githubSignIn = rememberFirebaseGithubSignInState(onResult = onFirebaseResult)
            Button(onClick = { githubSignIn.launch() }) { Text("Github Sign-In (Custom Design)") }

            //Facebook Sign-In with Custom Button and authentication with Firebase
            val facebookSignIn = rememberFirebaseFacebookSignInState(onResult = onFirebaseResult)
            Button(onClick = { facebookSignIn.launch() }) { Text("Facebook Sign-In (Custom Design)") }

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
        val googleSignIn = rememberFirebaseGoogleSignInState(onResult = onFirebaseResult)
        GoogleSignInButton(
            modifier = Modifier.fillMaxWidth().height(44.dp),
            fontSize = 19.sp
        ) { googleSignIn.launch() }

        //Apple Sign-In Button and authentication with Firebase
        val appleSignIn = rememberFirebaseAppleSignInState(onResult = onFirebaseResult)
        AppleSignInButton(modifier = Modifier.fillMaxWidth().height(44.dp)) { appleSignIn.launch() }

        //Facebook Sign-In Button and authentication with Firebase
        val facebookSignIn = rememberFirebaseFacebookSignInState(onResult = onFirebaseResult)
        FacebookSignInButton(
            modifier = Modifier.fillMaxWidth().height(44.dp),
            fontSize = 19.sp
        ) { facebookSignIn.launch() }

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
        val googleSignIn = rememberFirebaseGoogleSignInState(onResult = onFirebaseResult)
        GoogleSignInButtonIconOnly(onClick = { googleSignIn.launch() })

        //Apple Sign-In IconOnly Button and authentication with Firebase
        val appleSignIn = rememberFirebaseAppleSignInState(onResult = onFirebaseResult)
        AppleSignInButtonIconOnly(onClick = { appleSignIn.launch() })

        //Facebook Sign-In IconOnly Button and authentication with Firebase
        val facebookSignIn = rememberFirebaseFacebookSignInState(onResult = onFirebaseResult)
        FacebookSignInButtonIconOnly(onClick = { facebookSignIn.launch() })
    }
}
