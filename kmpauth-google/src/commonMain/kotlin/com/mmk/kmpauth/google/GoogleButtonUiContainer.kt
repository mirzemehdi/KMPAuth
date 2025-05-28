package com.mmk.kmpauth.google

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import com.mmk.kmpauth.core.KMPAuthInternalApi
import com.mmk.kmpauth.core.UiContainerScope
import com.mmk.kmpauth.core.logger.currentLogger
import com.mmk.kmpauth.google.GoogleAuthUiProvider.Companion.BASIC_AUTH_SCOPE
import kotlinx.coroutines.launch

@OptIn(KMPAuthInternalApi::class)
/**
 * GoogleButton Ui Container Composable that handles all sign-in functionality.
 * Make sure you create [GoogleAuthUiProvider] instance using [GoogleAuthProvider.create]
 * before invoking below composable function.
 *
 * Child of this Composable can be any view or Composable function.
 * You need to call [UiContainerScope.onClick] function on your child view's click function.
 *
 * [onGoogleSignInResult] callback will return [GoogleUser] or null if sign-in was unsuccessful.
 *
 * Example Usage:
 * ```
 * //Google Sign-In with Custom Button and authentication without Firebase
 * GoogleButtonUiContainer(onGoogleSignInResult = { googleUser ->
 *     val idToken = googleUser?.idToken // Send this idToken to your backend to verify
 * }) {
 *     Button(onClick = { this.onClick() }) { Text("Google Sign-In(Custom Design)") }
 * }
 *
 * ```
 *
 * @param filterByAuthorizedAccounts set to true so users can choose between available accounts to sign in.
 * @param scopes Custom scopes to retrieve more information. Default value listOf("email", "profile")
 * setting to false list any accounts that have previously been used to sign in to your app.
 */
@Composable
public fun GoogleButtonUiContainer(
    modifier: Modifier = Modifier,
    filterByAuthorizedAccounts: Boolean = false,
    scopes: List<String> = BASIC_AUTH_SCOPE,
    onGoogleSignInResult: (GoogleUser?) -> Unit,
    content: @Composable UiContainerScope.() -> Unit,
) {
    val googleAuthProvider = GoogleAuthProvider.get()
    val googleAuthUiProvider = googleAuthProvider.getUiProvider()
    val coroutineScope = rememberCoroutineScope()
    val updatedOnResultFunc by rememberUpdatedState(onGoogleSignInResult)

    val uiContainerScope = remember {
        object : UiContainerScope {
            override fun onClick() {
                currentLogger.log("GoogleUiButtonContainer is clicked")
                coroutineScope.launch {
                    val googleUser = googleAuthUiProvider.signIn(
                        filterByAuthorizedAccounts = filterByAuthorizedAccounts,
                        scopes = scopes
                    )
                    updatedOnResultFunc(googleUser)
                }
            }
        }
    }
    Box(modifier = modifier) { uiContainerScope.content() }

}