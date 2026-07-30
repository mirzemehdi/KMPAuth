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
import com.facebook.login.LoginConfiguration
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.mmk.kmpauth.core.KMPAuth
import com.mmk.kmpauth.core.KMPAuthInternalApi
import com.mmk.kmpauth.core.LaunchingSignInState
import com.mmk.kmpauth.core.SignInState
import com.mmk.kmpauth.core.getActivity
import com.mmk.kmpauth.core.logger.currentLogger
import kotlinx.coroutines.suspendCancellableCoroutine
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import kotlin.coroutines.resume


/**
 * Forwards an `Activity#onActivityResult` callback to the Facebook SDK.
 *
 * **Only required for [FacebookLoginTracking.Limited]** (the default). Limited
 * Login needs a nonce, which the Facebook SDK only accepts through
 * `LoginConfiguration`, and that API has no AndroidX Activity Result variant —
 * its results still arrive via `onActivityResult`. With
 * [FacebookLoginTracking.Enabled] KMPAuth uses the SDK's
 * `ActivityResultRegistryOwner` overload, so no override is needed.
 *
 * Calling this when it is not needed is harmless.
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
    loginTracking: FacebookLoginTracking,
    onResult: (Result<FacebookUser>) -> Unit,
): SignInState {
    val scope = rememberCoroutineScope()
    val activity = LocalContext.current.getActivity()
    val currentRequestScopes by rememberUpdatedState(requestScopes)
    val currentLoginTracking by rememberUpdatedState(loginTracking)
    val currentOnResult by rememberUpdatedState(onResult)

    return remember {
        LaunchingSignInState(scope) {
            val permissions: List<String> = currentRequestScopes.map {
                when (it) {
                    FacebookSignInRequestScope.Email -> "email"
                    FacebookSignInRequestScope.PublicProfile -> "public_profile"
                }
            }
            currentOnResult(signIn(activity, permissions, currentLoginTracking))
        }
    }
}

private suspend fun signIn(
    activity: ComponentActivity?,
    permissions: List<String>,
    loginTracking: FacebookLoginTracking,
): Result<FacebookUser> {
    if (activity == null) {
        return Result.failure(IllegalStateException("Activity is null"))
    }
    // Limited Login on Android has no tracking enum; it is selected by
    // supplying a nonce through LoginConfiguration. Facebook receives the
    // hashed nonce, while the raw nonce is handed to Firebase's OAuth provider
    // (mirroring the iOS flow).
    val rawNonce = if (loginTracking == FacebookLoginTracking.Limited) generateRawNonce() else null
    return try {
        suspendCancellableCoroutine { continuation ->
            loginManager.registerCallback(
                facebookLoginCallbackManager,
                facebookSignInCallback(loginTracking, rawNonce) { result ->
                    if (continuation.isActive) continuation.resume(result)
                }
            )
            when (loginTracking) {
                // Limited Login needs a nonce, which the SDK only accepts via
                // LoginConfiguration - an API with no ActivityResultRegistryOwner
                // overload, so its result still arrives through onActivityResult
                // (see KMPAuth.handleFacebookActivityResult).
                FacebookLoginTracking.Limited -> {
                    val config = LoginConfiguration(permissions, sha256(rawNonce!!))
                    loginManager.logIn(activity as Activity, config)
                }

                // Classic login goes through the AndroidX Activity Result APIs,
                // so callers do not need to override onActivityResult.
                FacebookLoginTracking.Enabled ->
                    loginManager.logInWithReadPermissions(
                        activity,
                        facebookLoginCallbackManager,
                        permissions,
                    )
            }
        }
    } finally {
        loginManager.unregisterCallback(facebookLoginCallbackManager)
    }
}

@OptIn(KMPAuthInternalApi::class)
private fun facebookSignInCallback(
    loginTracking: FacebookLoginTracking,
    rawNonce: String?,
    updatedOnResult: (Result<FacebookUser>) -> Unit
): FacebookCallback<LoginResult> = object : FacebookCallback<LoginResult> {
    override fun onSuccess(result: LoginResult) {
        currentLogger.log("Facebook Login successful")
        // Limited Login returns an OIDC authentication token (JWT) + the raw
        // nonce for Firebase's OAuth provider; classic login returns a
        // Graph-API access token and no nonce.
        val facebookUser = when (loginTracking) {
            FacebookLoginTracking.Limited -> FacebookUser(
                accessToken = result.authenticationToken?.token ?: "",
                nonce = rawNonce,
            )

            FacebookLoginTracking.Enabled -> FacebookUser(
                accessToken = result.accessToken.token,
                nonce = null,
            )
        }
        updatedOnResult(Result.success(facebookUser))
    }

    override fun onCancel() {
        updatedOnResult(Result.failure(IllegalStateException("Facebook sign-in cancelled")))
    }

    override fun onError(error: FacebookException) {
        updatedOnResult(Result.failure(IllegalStateException("Facebook sign-in failed with error: ${error.message}")))
    }
}

private fun generateRawNonce(length: Int = 32): String {
    val bytes = ByteArray(length)
    SecureRandom().nextBytes(bytes)
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
}

@OptIn(ExperimentalStdlibApi::class)
private fun sha256(input: String): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(input.encodeToByteArray())
    return digest.toHexString(HexFormat.Default)
}
