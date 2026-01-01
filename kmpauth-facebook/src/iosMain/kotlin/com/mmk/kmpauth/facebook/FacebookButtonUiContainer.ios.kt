package com.mmk.kmpauth.facebook

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import cocoapods.FBSDKLoginKit.FBSDKLoginConfiguration
import cocoapods.FBSDKLoginKit.FBSDKLoginManager
import cocoapods.FBSDKLoginKit.FBSDKLoginTrackingLimited
import com.mmk.kmpauth.core.KMPAuthInternalApi
import com.mmk.kmpauth.core.UiContainerScope
import com.mmk.kmpauth.core.logger.currentLogger
import io.ktor.util.generateNonce
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.refTo
import kotlinx.cinterop.usePinned
import platform.CoreCrypto.CC_SHA256
import platform.CoreCrypto.CC_SHA256_DIGEST_LENGTH
import platform.UIKit.UIApplication
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene

/**
 * FacebookButton Ui Container Composable that handles all sign-in functionality for Facebook.
 * Child of this Composable can be any view or Composable function.
 * You need to call [UiContainerScope.onClick] function on your child view's click function.
 *
 * [onResult] callback will return [Result] with [FirebaseUser] type.
 * @param requestScopes list of request scopes type of [FacebookSignInRequestScope].
 * @param linkAccount if true, it will link the account with the current user. Default value is false
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
@OptIn(ExperimentalForeignApi::class, KMPAuthInternalApi::class)
@Composable
public actual fun FacebookButtonUiContainer(
    modifier: Modifier,
    requestScopes: List<FacebookSignInRequestScope>,
    onResult: (Result<FacebookUser>) -> Unit,
    linkAccount: Boolean,
    content: @Composable UiContainerScope.() -> Unit,
) {
    val updatedOnResultFunc by rememberUpdatedState(onResult)

    val permissions: List<String> = requestScopes.map {
        when (it) {
            FacebookSignInRequestScope.Email -> "email"
            FacebookSignInRequestScope.PublicProfile -> "public_profile"
        }
    }

    val coroutineScope = rememberCoroutineScope()
    val uiContainerScope = remember {
        object : UiContainerScope {
            override fun onClick() {
                val loginManager = FBSDKLoginManager()

                val rootVCList = UIApplication.sharedApplication.connectedScenes.mapNotNull {
                    ((it as? UIWindowScene)?.windows?.firstOrNull() as? UIWindow)?.rootViewController
                }

                val rootVC = rootVCList.firstOrNull()
                if (rootVC == null) {
                    currentLogger.log("Root View Controller is null")
                    updatedOnResultFunc(Result.failure(IllegalStateException("Root View Controller is null")))
                    return
                }

                val nonce = generateNonce()

                loginManager.logInFromViewController(
                    rootVC,
                    FBSDKLoginConfiguration(
                        permissions = permissions,
                        tracking = FBSDKLoginTrackingLimited,
                        nonce = sha256(nonce),
                    ),
                    completion = { result, error ->
                        if (error != null) {
                            currentLogger.log("Facebook Login failed with error: ${error.localizedDescription}")
                            updatedOnResultFunc(Result.failure(IllegalStateException(error.localizedDescription)))
                            return@logInFromViewController
                        }
                        if (result?.isCancelled() == true) {
                            updatedOnResultFunc(Result.failure(IllegalStateException("User cancelled the login process")))
                            return@logInFromViewController
                        }

                        val facebookUser = FacebookUser(
                            accessToken = result?.authenticationToken()?.tokenString() ?: "",
                            nonce = nonce
                        )
                        updatedOnResultFunc(Result.success(facebookUser))
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