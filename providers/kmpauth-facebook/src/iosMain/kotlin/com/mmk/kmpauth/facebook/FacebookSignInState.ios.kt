package com.mmk.kmpauth.facebook

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import com.mmk.kmpauth.core.KMPAuthInternalApi
import com.mmk.kmpauth.core.LaunchingSignInState
import com.mmk.kmpauth.core.SignInState
import com.mmk.kmpauth.core.logger.currentLogger
import io.ktor.util.generateNonce
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.refTo
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.CoreCrypto.CC_SHA256
import platform.CoreCrypto.CC_SHA256_DIGEST_LENGTH
import platform.UIKit.UIApplication
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene
import swiftPMImport.io.github.mirzemehdi.providers.kmpauth.facebook.FBSDKLoginConfiguration
import swiftPMImport.io.github.mirzemehdi.providers.kmpauth.facebook.FBSDKLoginManager
import swiftPMImport.io.github.mirzemehdi.providers.kmpauth.facebook.FBSDKLoginTrackingLimited
import kotlin.coroutines.resume

@OptIn(KMPAuthInternalApi::class)
@Composable
public actual fun rememberFacebookSignInState(
    requestScopes: List<FacebookSignInRequestScope>,
    linkAccount: Boolean,
    onResult: (Result<FacebookUser>) -> Unit,
): SignInState {
    val scope = rememberCoroutineScope()
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
            currentOnResult(signIn(permissions))
        }
    }
}

@OptIn(ExperimentalForeignApi::class, KMPAuthInternalApi::class)
private suspend fun signIn(permissions: List<String>): Result<FacebookUser> {
    val loginManager = FBSDKLoginManager()

    val rootVCList = UIApplication.sharedApplication.connectedScenes.mapNotNull {
        ((it as? UIWindowScene)?.windows?.firstOrNull() as? UIWindow)?.rootViewController
    }

    val rootVC = rootVCList.firstOrNull()
    if (rootVC == null) {
        currentLogger.log("Root View Controller is null")
        return Result.failure(IllegalStateException("Root View Controller is null"))
    }

    val nonce = generateNonce()

    return suspendCancellableCoroutine { continuation ->
        fun resumeOnce(result: Result<FacebookUser>) {
            if (continuation.isActive) continuation.resume(result)
        }

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
                    resumeOnce(Result.failure(IllegalStateException(error.localizedDescription)))
                    return@logInFromViewController
                }
                if (result?.isCancelled() == true) {
                    resumeOnce(Result.failure(IllegalStateException("User cancelled the login process")))
                    return@logInFromViewController
                }

                val facebookUser = FacebookUser(
                    accessToken = result?.authenticationToken()?.tokenString() ?: "",
                    nonce = nonce
                )
                resumeOnce(Result.success(facebookUser))
            }
        )
    }
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
