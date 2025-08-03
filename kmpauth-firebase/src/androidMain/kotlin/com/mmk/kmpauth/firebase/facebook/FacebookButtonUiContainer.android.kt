package com.mmk.kmpauth.firebase.facebook

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

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.FacebookAuthProvider
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException


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
    onResult: (Result<FirebaseUser?>) -> Unit,
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
    updatedOnResult: (Result<FirebaseUser?>) -> Unit
): FacebookCallback<LoginResult> = object : FacebookCallback<LoginResult> {
    override fun onSuccess(result: LoginResult) {
        currentLogger.log("Facebook Login successful, attempting to sign in with Firebase")
        val accessToken = result.accessToken.token
        val authCredential = FacebookAuthProvider.credential(accessToken)
        coroutineScope.launch {
            try {
                val auth = Firebase.auth
                val currentUser = auth.currentUser
                val firebaseAuthResult = if (linkAccount && currentUser != null) {
                    currentLogger.log("Linking Facebook account with current firebase user: ${currentUser.uid}")
                    currentUser.linkWithCredential(authCredential)
                } else {
                    currentLogger.log("Signing in with Facebook account on Firebase")
                    auth.signInWithCredential(authCredential)
                }
                val user = firebaseAuthResult.user
                if (user == null) {
                    currentLogger.log("Firebase sign-in failed: Firebase user is null")
                    updatedOnResult(Result.failure(IllegalStateException("Firebase user is null")))
                } else {
                    currentLogger.log("Firebase sign-in successful")
                    updatedOnResult(Result.success(user))
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e

                currentLogger.log("Firebase sign-in failed with error: ${e.message}")
                updatedOnResult(Result.failure(e))
            }
        }
    }

    override fun onCancel() {
        updatedOnResult(Result.failure(IllegalStateException("Facebook sign-in cancelled")))
    }

    override fun onError(error: FacebookException) {
        updatedOnResult(Result.failure(IllegalStateException("Facebook sign-in failed with error: ${error.message}")))
    }
}