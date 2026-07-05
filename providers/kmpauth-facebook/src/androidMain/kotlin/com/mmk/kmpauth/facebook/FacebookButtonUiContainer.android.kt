package com.mmk.kmpauth.facebook

import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.mmk.kmpauth.core.KMPAuth
import com.mmk.kmpauth.core.KMPAuthInternalApi
import com.mmk.kmpauth.core.UiContainerScope
import com.mmk.kmpauth.core.getActivity
import com.mmk.kmpauth.core.logger.currentLogger
import kotlinx.coroutines.CoroutineScope


/**
 * You mush call `KMPAuth.handleFacebookActivityResult` from your Activity's onActivityResult to handle Facebook login.
 *
 * Example:
 * ```
 * override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
 *   super.onActivityResult(requestCode, resultCode, data)
 *   KMPAuth.handleFacebookActivityResult(requestCode, resultCode, data)
 * }
 * ```
 */
public fun KMPAuth.handleFacebookActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    facebookLoginCallbackManager.onActivityResult(requestCode, resultCode, data)
}


private val facebookLoginCallbackManager: CallbackManager by lazy { CallbackManager.Factory.create() }

private val loginManager: LoginManager by lazy { LoginManager.getInstance() }

@OptIn(KMPAuthInternalApi::class)
@Composable
public actual fun FacebookButtonUiContainer(
    modifier: Modifier,
    requestScopes: List<FacebookSignInRequestScope>,
    onResult: (Result<FacebookUser>) -> Unit,
    linkAccount: Boolean,
    content: @Composable (UiContainerScope.() -> Unit)
) {
    val updatedOnResult by rememberUpdatedState(onResult)
    val coroutineScope = rememberCoroutineScope()
    val activity = LocalContext.current.getActivity()

    DisposableEffect(Unit) {
        loginManager.registerCallback(
            facebookLoginCallbackManager,
            facebookSignInCallback(coroutineScope, linkAccount, updatedOnResult)
        )

        onDispose {
            loginManager.unregisterCallback(facebookLoginCallbackManager)
        }
    }

    val permissions: List<String> = requestScopes.map {
        when (it) {
            FacebookSignInRequestScope.Email -> "email"
            FacebookSignInRequestScope.PublicProfile -> "public_profile"
        }
    }

    val uiContainerScope = remember {
        object : UiContainerScope {
            override fun onClick() {
                if (activity == null) {
                    updatedOnResult(Result.failure(IllegalStateException("Activity is null")))
                    return
                }
                loginManager.logInWithReadPermissions(activity as Activity, permissions)
            }
        }
    }
    Box(modifier = modifier) { uiContainerScope.content() }
}

@OptIn(KMPAuthInternalApi::class)
private fun facebookSignInCallback(
    coroutineScope: CoroutineScope,
    linkAccount: Boolean,
    updatedOnResult: (Result<FacebookUser>) -> Unit
): FacebookCallback<LoginResult> = object : FacebookCallback<LoginResult> {
    override fun onSuccess(result: LoginResult) {
        currentLogger.log("Facebook Login successful")
        val facebookUser = FacebookUser(
            accessToken = result.accessToken.token,
            nonce = result.authenticationToken?.expectedNonce
        )
        updatedOnResult(Result.success(facebookUser))
    }

    override fun onCancel() {
        updatedOnResult(Result.failure(IllegalStateException("Facebook sign-in cancelled")))
    }

    override fun onError(error: FacebookException) {
        updatedOnResult(Result.failure(IllegalStateException("Facebook sign-in failed with error: ${error.message}")))
    }
}