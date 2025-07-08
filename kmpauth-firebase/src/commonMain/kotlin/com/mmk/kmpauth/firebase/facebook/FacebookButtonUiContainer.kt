package com.mmk.kmpauth.firebase.facebook

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mmk.kmpauth.core.UiContainerScope
import com.mmk.kmpauth.firebase.apple.AppleSignInRequestScope
import com.mmk.kmpauth.firebase.oauth.OAuthContainer
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.OAuthProvider

/**
 * FacebookButton Ui Container Composable that handles all sign-in functionality for Facebook.
 * Child of this Composable can be any view or Composable function.
 * You need to call [UiContainerScope.onClick] function on your child view's click function.
 *
 * [onResult] callback will return [Result] with [FirebaseUser] type.
 * @param requestScopes Request Scopes that is provided in Facebook OAuth. By Default, user's email is requested.
 * @param customParameters Custom Parameters that is provided in Facebook OAuth.
 * @param linkAccount [Boolean] flag to link account with current user. Default value is false.
 *
 * Example Usage:
 * ```
 * //Facebook Sign-In with Custom Button and authentication with Firebase
 * FacebookButtonUiContainer(onResult = onFirebaseResult) {
 *     Button(onClick = { this.onClick() }) { Text("Facebook Sign-In (Custom Design)") }
 * }
 *
 * ```
 *
 */
@Composable
public expect fun FacebookButtonUiContainer(
    modifier: Modifier = Modifier,
    requestScopes: List<FacebookSignInRequestScope> = listOf(
        FacebookSignInRequestScope.FullName,
        FacebookSignInRequestScope.Email
    ),
    onResult: (Result<FirebaseUser?>) -> Unit,
    linkAccount: Boolean = false,
    content: @Composable UiContainerScope.() -> Unit,
)

@Deprecated(
    "Use AppleButtonUiContainer with the linkAccount parameter, which defaults to false.",
    ReplaceWith(""),
    DeprecationLevel.WARNING
)
@Composable
public expect fun FacebookButtonUiContainer(
    modifier: Modifier = Modifier,
    requestScopes: List<FacebookSignInRequestScope> = listOf(
        FacebookSignInRequestScope.FullName,
        FacebookSignInRequestScope.Email
    ),
    onResult: (Result<FirebaseUser?>) -> Unit,
    content: @Composable UiContainerScope.() -> Unit,
)