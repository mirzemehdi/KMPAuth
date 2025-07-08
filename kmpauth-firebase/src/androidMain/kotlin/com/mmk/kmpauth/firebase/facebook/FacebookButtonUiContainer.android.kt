package com.mmk.kmpauth.firebase.facebook

import android.app.Activity
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
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
import com.mmk.kmpauth.core.KMPAuthInternalApi
import com.mmk.kmpauth.core.UiContainerScope
import com.mmk.kmpauth.core.getActivity

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.ActionCodeSettings
import dev.gitlive.firebase.auth.FacebookAuthProvider
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.auth
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

public val callbackManager: CallbackManager = CallbackManager.Factory.create()

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

    LoginManager.getInstance().registerCallback(
        callbackManager,
        object : FacebookCallback<LoginResult> {
            override fun onSuccess(loginResult: LoginResult) {
                val accessToken = loginResult.accessToken.token
                Log.i("FacebookButtonUiContainer","Facebook access token: $accessToken")
                val authCredential = FacebookAuthProvider.credential(accessToken)
                Log.i("FacebookButtonUiContainer","Facebook auth credential: $authCredential")
                coroutineScope.launch {
                    try {
                        val auth = Firebase.auth
                        val currentUser = auth.currentUser
                        val result = if (linkAccount && currentUser != null) {
                            Log.i("FacebookButtonUiContainer","Linking Facebook account with current user: ${currentUser.uid}")
                            currentUser.linkWithCredential(authCredential)
                        } else {
                            Log.i("FacebookButtonUiContainer","Signing in with Facebook account")
                            auth.signInWithCredential(authCredential)
                        }
                        val user = result.user
                        if (user == null) {
                            Log.i("FacebookButtonUiContainer","Firebase Null user after Facebook sign-in")
                            updatedOnResult(Result.failure(IllegalStateException("Firebase Null user")))
                        }
                        else {
                            updatedOnResult(Result.success(user))
                        }
                        Log.i("FacebookButtonUiContainer","Facebook sign-in successful: ${result.user?.uid}")
                    } catch (e: Exception) {
                        Log.i("FacebookButtonUiContainer","Facebook sign-in failed: ${e.message}")
                        if (e is CancellationException) throw e
                        updatedOnResult(Result.failure(e))
                    }
                }
            }

            override fun onCancel() {
                updatedOnResult(Result.failure(IllegalStateException("Facebook sign-in cancelled")))
            }

            override fun onError(error: FacebookException) {
                updatedOnResult(Result.failure(IllegalStateException("Facebook sign-in error: ${error.message}")))
            }
        })

    val uiContainerScope = remember {
        object : UiContainerScope {
            override fun onClick() {
                LoginManager.getInstance().logInWithReadPermissions(activity as Activity, listOf("email", "public_profile"))
            }
        }
    }
    Box(modifier = modifier) { uiContainerScope.content() }
}

@Deprecated(
    message = "Use AppleButtonUiContainer with the linkAccount parameter, which defaults to false.",
    replaceWith = ReplaceWith(expression = ""),
    level = DeprecationLevel.WARNING
)
@Composable
public actual fun FacebookButtonUiContainer(
    modifier: Modifier,
    requestScopes: List<FacebookSignInRequestScope>,
    onResult: (Result<FirebaseUser?>) -> Unit,
    content: @Composable (UiContainerScope.() -> Unit)
) {
    FacebookButtonUiContainer(modifier,requestScopes,onResult,false) { }
}