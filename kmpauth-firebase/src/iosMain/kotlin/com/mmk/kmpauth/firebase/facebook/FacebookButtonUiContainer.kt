package com.mmk.kmpauth.firebase.facebook

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import cocoapods.FBSDKLoginKit.FBSDKAccessToken
import cocoapods.FBSDKLoginKit.FBSDKLoginConfiguration
import cocoapods.FBSDKLoginKit.FBSDKLoginManager
import cocoapods.FBSDKLoginKit.FBSDKLoginTrackingEnabled
import cocoapods.FBSDKLoginKit.FBSDKLoginTrackingLimited
import cocoapods.FirebaseAuth.FIRAuth
import cocoapods.FirebaseAuth.FIRAuthCredential
import cocoapods.FirebaseAuth.FIRAuthDataResult
import cocoapods.FirebaseAuth.FIROAuthProvider
import com.mmk.kmpauth.core.UiContainerScope
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.FacebookAuthProvider
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.OAuthCredential
import dev.gitlive.firebase.auth.OAuthProvider
import dev.gitlive.firebase.auth.auth
import io.ktor.util.generateNonce
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.refTo
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import platform.CoreCrypto.CC_SHA256
import platform.CoreCrypto.CC_SHA256_DIGEST_LENGTH
import platform.Foundation.NSError
import platform.UIKit.UIApplication
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene

/**
 * AppleButton Ui Container Composable that handles all sign-in functionality for Apple.
 * Child of this Composable can be any view or Composable function.
 * You need to call [UiContainerScope.onClick] function on your child view's click function.
 *
 * [onResult] callback will return [Result] with [FirebaseUser] type.
 * @param requestScopes list of request scopes type of [AppleSignInRequestScope].
 * @param linkAccount if true, it will link the account with the current user. Default value is false
 * Example Usage:
 * ```
 * //Apple Sign-In with Custom Button and authentication with Firebase
 * AppleButtonUiContainer(onResult = onFirebaseResult) {
 *     Button(onClick = { this.onClick() }) { Text("Apple Sign-In (Custom Design)") }
 * }
 *
 * ```
 *
 */
@OptIn(ExperimentalForeignApi::class)
@Composable
public actual fun FacebookButtonUiContainer(
    modifier: Modifier,
    requestScopes: List<FacebookSignInRequestScope>,
    onResult: (Result<FirebaseUser?>) -> Unit,
    linkAccount: Boolean,
    content: @Composable UiContainerScope.() -> Unit,
) {
    val updatedOnResultFunc by rememberUpdatedState(onResult)

    val rememberCoroutine = rememberCoroutineScope()
    val uiContainerScope = remember {
        object : UiContainerScope {
            override fun onClick() {
                val loginManager = FBSDKLoginManager()

                val rootVCList = UIApplication.sharedApplication.connectedScenes.mapNotNull {
                    ((it as? UIWindowScene)?.windows?.firstOrNull() as? UIWindow)?.rootViewController
                }

                val rootVC = rootVCList.firstOrNull()
                if (rootVC == null) {
                    updatedOnResultFunc(Result.failure(IllegalStateException("Root View Controller is null")))
                    return
                }

                val nonce = generateNonce()

                loginManager.logInFromViewController(
                    rootVC,
                    FBSDKLoginConfiguration(
                        permissions = listOf(
                            "email",
                            "public_profile"
                        ),
                        tracking = FBSDKLoginTrackingLimited,
                        nonce = sha256(nonce),
                    ),
                    completion = { result, error ->
                        if (error != null) {
                            updatedOnResultFunc(Result.failure(IllegalStateException(error.localizedDescription)))
                            return@logInFromViewController
                        }
                        if (result?.isCancelled() == true) {
                            updatedOnResultFunc(Result.failure(IllegalStateException("User cancelled the login process")))
                            return@logInFromViewController
                        }

                        rememberCoroutine.launch {
                            val accessToken = result?.authenticationToken()?.tokenString() ?: ""
                            println("FB access Token: $accessToken")

                            println("nonce: " + result?.authenticationToken()?.nonce())

                            val credential = OAuthProvider.credential(
                                providerId = "facebook.com",
                                idToken = accessToken,
                                rawNonce = nonce
                            )

                            val auth = Firebase.auth
                            val currentUser = auth.currentUser

                            try {

                                val result = if (linkAccount && currentUser != null) {
                                    currentUser.linkWithCredential(credential)
                                } else {
                                    auth.signInWithCredential(credential)
                                }
                                updatedOnResultFunc(Result.success(result.user))

                            } catch (e: Exception) {
                                updatedOnResultFunc(
                                    Result.failure(e)
                                )

                            }
                        }
                    }
                )
            }
        }
    }

    Box(modifier = modifier) { uiContainerScope.content() }
}

@OptIn(ExperimentalForeignApi::class, ExperimentalStdlibApi::class)
private fun sha256(input: String): String {
    val hashedData = UByteArray(CC_SHA256_DIGEST_LENGTH)
    val inputData = input.encodeToByteArray()
    inputData.usePinned {
        CC_SHA256(it.addressOf(0), inputData.size.convert(), hashedData.refTo(0))
    }
    return hashedData.toByteArray().toHexString(HexFormat.Default)
}

@Deprecated(
    "Use AppleButtonUiContainer with the linkAccount parameter, which defaults to false.",
    ReplaceWith(""),
    DeprecationLevel.WARNING
)
@Composable
public actual fun FacebookButtonUiContainer(
    modifier: Modifier,
    requestScopes: List<FacebookSignInRequestScope>,
    onResult: (Result<FirebaseUser?>) -> Unit,
    content: @Composable UiContainerScope.() -> Unit,
) {
    FacebookButtonUiContainer(modifier, requestScopes, onResult, false, content)
}