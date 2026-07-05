package com.mmk.kmpauth.facebook

import android.app.Activity
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.mmk.kmpauth.core.KMPAuth
import com.mmk.kmpauth.core.KMPAuthInternalApi
import com.mmk.kmpauth.core.LaunchingSignInState
import com.mmk.kmpauth.core.SignInState
import com.mmk.kmpauth.core.getActivity
import com.mmk.kmpauth.core.logger.currentLogger
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume


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
public actual fun rememberFacebookSignInState(
    requestScopes: List<FacebookSignInRequestScope>,
    linkAccount: Boolean,
    onResult: (Result<FacebookUser>) -> Unit,
): SignInState {
    val scope = rememberCoroutineScope()
    val activity = LocalContext.current.getActivity()
    val currentRequestScopes by rememberUpdatedState(requestScopes)
    val currentOnResult by rememberUpdatedState(onResult)

    return remember {
        LaunchingSignInState(scope) {
            val permissions: List<String> = currentRequestScopes.map {
                when (it) {
                    FacebookSignInRequestScope.Email -> "email"
                    FacebookSignInRequestScope.PublicProfile -> "public_profile"
                }
            }
            currentOnResult(signIn(activity, permissions))
        }
    }
}

private suspend fun signIn(
    activity: ComponentActivity?,
    permissions: List<String>,
): Result<FacebookUser> {
    if (activity == null) {
        return Result.failure(IllegalStateException("Activity is null"))
    }
    return try {
        suspendCancellableCoroutine { continuation ->
            loginManager.registerCallback(
                facebookLoginCallbackManager,
                facebookSignInCallback { result ->
                    if (continuation.isActive) continuation.resume(result)
                }
            )
            loginManager.logInWithReadPermissions(activity as Activity, permissions)
        }
    } finally {
        loginManager.unregisterCallback(facebookLoginCallbackManager)
    }
}

@OptIn(KMPAuthInternalApi::class)
private fun facebookSignInCallback(
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
