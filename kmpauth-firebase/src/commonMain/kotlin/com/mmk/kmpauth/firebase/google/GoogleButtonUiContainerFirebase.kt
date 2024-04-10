package com.mmk.kmpauth.firebase.google

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import com.mmk.kmpauth.core.UiContainerScope
import com.mmk.kmpauth.google.GoogleButtonUiContainer
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.GoogleAuthProvider
import dev.gitlive.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * GoogleSignInButton Ui Container Composable that handles all sign-in functionality for Google.
 * Child of this Composable can be any view or Composable function.
 * You need to call [UiContainerScope.onClick] function on your child view's click function.
 *
 * [onResult] callback will return [Result] with [FirebaseUser] type.
 *
 * Example Usage:
 * ```
 * //Github Sign-In with Custom Button and authentication with Firebase
 * GoogleButtonUiContainerFirebase(onResult = onFirebaseResult) {
 *     Button(onClick = { this.onClick() }) { Text("Google Sign-In (Custom Design)") }
 * }
 *
 * ```
 *
 */
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
        if (idToken == null) {
            updatedOnResult(Result.failure(IllegalStateException("Idtoken is null")))
            return@GoogleButtonUiContainer
        }
        val authCredential = GoogleAuthProvider.credential(idToken, accessToken)
        coroutineScope.launch {
            try {
                val result = Firebase.auth.signInWithCredential(authCredential)
                if (result.user == null) updatedOnResult(Result.failure(IllegalStateException("Firebase Null user")))
                else updatedOnResult(Result.success(result.user))
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                updatedOnResult(Result.failure(e))
            }
        }

    }, content = content)

}