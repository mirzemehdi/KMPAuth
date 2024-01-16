package com.mmk.kmpauth.firebase.google

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import com.mmk.kmpauth.core.UiContainerScope
import com.mmk.kmpauth.firebase.oauth.OAuthContainer
import com.mmk.kmpauth.google.GoogleButtonUiContainer
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.GoogleAuthProvider
import dev.gitlive.firebase.auth.OAuthProvider
import dev.gitlive.firebase.auth.auth
import kotlinx.coroutines.launch

@Composable
public fun GoogleButtonUiContainerFirebase(
    modifier: Modifier = Modifier,
    onResult: (Result<FirebaseUser?>) -> Unit,
    content: @Composable UiContainerScope.() -> Unit,
) {

    val updatedOnResult by rememberUpdatedState(onResult)
    val coroutineScope = rememberCoroutineScope()
    GoogleButtonUiContainer(modifier = modifier, onGoogleSignInResult = { googleUser ->
        val idToken = googleUser?.idToken
        val accessToken = googleUser?.accessToken
        val authCredential = GoogleAuthProvider.credential(idToken, accessToken)
        coroutineScope.launch {
            val result = Firebase.auth.signInWithCredential(authCredential)
            if (result.user == null) updatedOnResult(Result.failure(IllegalStateException("Firebase Null user")))
            else updatedOnResult(Result.success(result.user))
        }

    }, content = content)

}