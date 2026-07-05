package com.mmk.kmpauth.firebase.google

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import com.mmk.kmpauth.core.UiContainerScope
import com.mmk.kmpauth.core.auth.KMPAuthBackend
import com.mmk.kmpauth.firebase.backend.ensureFirebaseBackendRegistered
import com.mmk.kmpauth.google.GoogleButtonUiContainer
import dev.gitlive.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch

/**
 * GoogleSignInButton Ui Container Composable that handles all sign-in functionality for Google.
 * Child of this Composable can be any view or Composable function.
 * You need to call [UiContainerScope.onClick] function on your child view's click function.
 *
 * @param linkAccount Default value is false
 * @param filterByAuthorizedAccounts set to true so users can choose between available accounts to sign in.
 * @param scopes Custom scopes to retrieve more information. Default value listOf("email", "profile")
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
    linkAccount: Boolean = false,
    filterByAuthorizedAccounts: Boolean = false,
    isAutoSelectEnabled: Boolean = true,
    scopes: List<String> = listOf("email", "profile"),
    onResult: (Result<FirebaseUser?>) -> Unit,
    content: @Composable UiContainerScope.() -> Unit,
) {

    val updatedOnResult by rememberUpdatedState(onResult)
    val coroutineScope = rememberCoroutineScope()
    val signInHandler = remember {
        ensureFirebaseBackendRegistered()
        GoogleFirebaseSignInHandler(backend = KMPAuthBackend.require())
    }
    GoogleButtonUiContainer(
        modifier = modifier,
        filterByAuthorizedAccounts = filterByAuthorizedAccounts,
        isAutoSelectEnabled = isAutoSelectEnabled,
        scopes = scopes,
        onGoogleSignInResult = { googleUser ->
            coroutineScope.launch {
                updatedOnResult(signInHandler.signIn(googleUser, linkAccount))
            }
        },
        content = content
    )

}

@Deprecated(
    "Use GoogleButtonUiContainerFirebase with linkAccount and filterByAuthorizedAccounts parameters, which defaults to false",
    ReplaceWith(""),
    DeprecationLevel.WARNING
)
@Composable
public fun GoogleButtonUiContainerFirebase(
    modifier: Modifier = Modifier,
    onResult: (Result<FirebaseUser?>) -> Unit,
    content: @Composable UiContainerScope.() -> Unit,
) {
    GoogleButtonUiContainerFirebase(
        modifier = modifier,
        linkAccount = false,
        filterByAuthorizedAccounts = false,
        onResult = onResult,
        content = content
    )
}

