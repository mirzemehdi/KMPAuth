package com.mmk.kmpauth.firebase.apple

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
import kotlinx.coroutines.withContext


/**
 * AppleButton Ui Container Composable that handles all sign-in functionality for Apple.
 * Child of this Composable can be any view or Composable function.
 * You need to call [UiContainerScope.onClick] function on your child view's click function.
 *
 * [onResult] callback will return [Result] with [FirebaseUser] type.
 * @param requestScopes list of request scopes type of [AppleSignInRequestScope].
 * Example Usage:
 * ```
 * //Apple Sign-In with Custom Button and authentication with Firebase
 * AppleButtonUiContainer(onResult = onFirebaseResult) {
 *     Button(onClick = { this.onClick() }) { Text("Apple Sign-In (Custom Design)") }
 * }
 *
 * ```
 *
 */
@Composable
public actual fun AppleButtonUiContainer(
    modifier: Modifier,
    requestScopes: List<AppleSignInRequestScope>,
    onResult: (Result<FirebaseUser?>) -> Unit,
    content: @Composable UiContainerScope.() -> Unit,
) {
    val oAuthProvider = remember { mutableStateOf<List<OAuthProvider>>(emptyList()) }
    val oathProviderRequestScopes = requestScopes.map {
        when (it) {
            AppleSignInRequestScope.Email -> "email"
            AppleSignInRequestScope.FullName -> "name"
        }
    }
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
                OAuthProvider(provider = "apple.com", scopes = oathProviderRequestScopes)
            )
        }
    }
}