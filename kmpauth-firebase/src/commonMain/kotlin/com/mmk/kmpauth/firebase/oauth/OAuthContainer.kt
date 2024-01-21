package com.mmk.kmpauth.firebase.oauth

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mmk.kmpauth.core.UiContainerScope
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.OAuthProvider

/**
 * OAuth Ui Container Composable that handles all sign-in functionality for given provider.
 * Child of this Composable can be any view or Composable function.
 * You need to call [UiContainerScope.onClick] function on your child view's click function.
 *
 * [onResult] callback will return [Result] with [FirebaseUser] type.
 * @param oAuthProvider [OAuthProvider] class object.
 *
 * Example Usage:
 * ```
 *
 * OAuthContainer(onResult = onFirebaseResult) {
 *     Button(onClick = { this.onClick() }) { Text("Github Sign-In (Custom Design)") }
 * }
 * val oAuthProvider = OAuthProvider(provider = "github.com")
 * OAuthContainer(modifier = modifier, oAuthProvider = oAuthProvider,onResult = onFirebaseResult){
 *  Button(onClick = { this.onClick() }) { Text("Github Sign-In (Custom Design)") }
 * }
 *
 * ```
 *
 */
@Composable
public expect fun OAuthContainer(
    modifier: Modifier = Modifier,
    oAuthProvider: OAuthProvider,
    onResult: (Result<FirebaseUser?>) -> Unit,
    content: @Composable UiContainerScope.() -> Unit,
)