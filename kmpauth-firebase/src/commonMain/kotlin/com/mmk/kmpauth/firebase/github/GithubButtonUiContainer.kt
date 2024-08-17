package com.mmk.kmpauth.firebase.github

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.mmk.kmpauth.core.UiContainerScope
import com.mmk.kmpauth.firebase.oauth.OAuthContainer
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.OAuthProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

/**
 * GithubButton Ui Container Composable that handles all sign-in functionality for Github.
 * Child of this Composable can be any view or Composable function.
 * You need to call [UiContainerScope.onClick] function on your child view's click function.
 *
 * [onResult] callback will return [Result] with [FirebaseUser] type.
 * @param requestScopes Request Scopes that is provided in Github OAuth. By Default, user's email is requested.
 * @param customParameters Custom Parameters that is provided in Github OAuth.
 *
 * Example Usage:
 * ```
 * //Github Sign-In with Custom Button and authentication with Firebase
 * GithubButtonUiContainer(onResult = onFirebaseResult) {
 *     Button(onClick = { this.onClick() }) { Text("Github Sign-In (Custom Design)") }
 * }
 *
 * ```
 *
 */
@Composable
public fun GithubButtonUiContainer(
    modifier: Modifier = Modifier,
    requestScopes: List<String> = listOf("user:email"),
    customParameters: Map<String, String> = emptyMap(),
    onResult: (Result<FirebaseUser?>) -> Unit,
    content: @Composable UiContainerScope.() -> Unit,
) {
    val oAuthProvider = remember { mutableStateOf<List<OAuthProvider>>(emptyList()) }
    oAuthProvider.value.firstOrNull()?.let {
        OAuthContainer(
            modifier = modifier,
            oAuthProvider = it,
            onResult = onResult,
            content = content
        )
    }
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            oAuthProvider.value = listOf(
                OAuthProvider(
                    provider = "github.com",
                    scopes = requestScopes,
                    customParameters = customParameters
                )
            )
        }
    }
}