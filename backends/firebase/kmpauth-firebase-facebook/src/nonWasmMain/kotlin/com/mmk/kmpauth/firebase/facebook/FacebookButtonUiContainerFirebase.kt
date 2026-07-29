package com.mmk.kmpauth.firebase.facebook

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.mmk.kmpauth.core.UiContainerScope
import com.mmk.kmpauth.core.auth.KMPAuthBackend
import com.mmk.kmpauth.facebook.FacebookLoginTracking
import com.mmk.kmpauth.facebook.FacebookSignInRequestScope
import com.mmk.kmpauth.facebook.rememberFacebookAuthState
import com.mmk.kmpauth.firebase.backend.FirebaseAuthBackend
import dev.gitlive.firebase.auth.FirebaseUser

/**
 * Legacy container API for Facebook Sign-In with Firebase. Superseded by
 * [rememberFacebookAuthState] (in `kmpauth-facebook`), which returns a
 * [com.mmk.kmpauth.core.SignInState] you can wire to any clickable without
 * the receiver-scope indirection.
 */
@Deprecated(
    "Use rememberFacebookAuthState(...) from kmpauth-facebook and call launch() from your own " +
        "button's onClick. Scheduled for removal in 4.0.",
    ReplaceWith(
        "rememberFacebookAuthState(requestScopes, linkAccount, loginTracking, onResult)",
        "com.mmk.kmpauth.facebook.rememberFacebookAuthState"
    ),
    DeprecationLevel.WARNING
)
@Composable
public fun FacebookButtonUiContainerFirebase(
    modifier: Modifier = Modifier,
    requestScopes: List<FacebookSignInRequestScope> = listOf(
        FacebookSignInRequestScope.PublicProfile,
        FacebookSignInRequestScope.Email
    ),
    onResult: (Result<FirebaseUser?>) -> Unit,
    linkAccount: Boolean = false,
    loginTracking: FacebookLoginTracking = FacebookLoginTracking.Limited,
    content: @Composable UiContainerScope.() -> Unit,
) {
    // 2.x apps never registered a backend explicitly; keep the container
    // zero-config by lazily registering the Firebase default.
    KMPAuthBackend.register(FirebaseAuthBackend)
    val signInState = rememberFacebookAuthState(
        requestScopes = requestScopes,
        linkAccount = linkAccount,
        loginTracking = loginTracking,
        // The state reports KMPAuthUser; this 2.x-compat container keeps its
        // Result<FirebaseUser?> callback by unwrapping the native user.
        onResult = { result -> onResult(result.map { it.raw as? FirebaseUser }) },
    )
    val uiContainerScope = remember(signInState) {
        object : UiContainerScope {
            override fun onClick() = signInState.launch()
        }
    }
    Box(modifier = modifier) { uiContainerScope.content() }
}
