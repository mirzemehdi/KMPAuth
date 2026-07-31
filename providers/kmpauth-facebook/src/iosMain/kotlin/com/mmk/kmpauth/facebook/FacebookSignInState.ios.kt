package com.mmk.kmpauth.facebook

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import com.mmk.kmpauth.core.KMPAuthInternalApi
import com.mmk.kmpauth.core.LaunchingSignInState
import com.mmk.kmpauth.core.SignInState
import com.mmk.kmpauth.core.auth.KMPAuthUserCancelledException
import com.mmk.kmpauth.core.logger.currentLogger
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UByteVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.refTo
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.CoreCrypto.CC_SHA256
import platform.CoreCrypto.CC_SHA256_DIGEST_LENGTH
import platform.Security.SecRandomCopyBytes
import platform.Security.errSecSuccess
import platform.Security.kSecRandomDefault
import platform.UIKit.UIApplication
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene
import swiftPMImport.io.github.mirzemehdi.providers.kmpauth.facebook.FBSDKLoginConfiguration
import swiftPMImport.io.github.mirzemehdi.providers.kmpauth.facebook.FBSDKLoginManager
import swiftPMImport.io.github.mirzemehdi.providers.kmpauth.facebook.FBSDKLoginTrackingEnabled
import swiftPMImport.io.github.mirzemehdi.providers.kmpauth.facebook.FBSDKLoginTrackingLimited
import kotlin.coroutines.resume

@OptIn(KMPAuthInternalApi::class)
@Composable
public actual fun rememberFacebookSignInState(
    requestScopes: List<FacebookSignInRequestScope>,
    linkAccount: Boolean,
    loginTracking: FacebookLoginTracking,
    onResult: (Result<FacebookUser>) -> Unit,
): SignInState {
    val scope = rememberCoroutineScope()
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
            currentOnResult(signIn(permissions, currentLoginTracking))
        }
    }
}

@OptIn(ExperimentalForeignApi::class, KMPAuthInternalApi::class)
private suspend fun signIn(
    permissions: List<String>,
    loginTracking: FacebookLoginTracking,
): Result<FacebookUser> {
    val loginManager = FBSDKLoginManager()

    val rootVCList = UIApplication.sharedApplication.connectedScenes.mapNotNull {
        ((it as? UIWindowScene)?.windows?.firstOrNull() as? UIWindow)?.rootViewController
    }

    val rootVC = rootVCList.firstOrNull()
    if (rootVC == null) {
        currentLogger.log("Root View Controller is null")
        return Result.failure(IllegalStateException("Root View Controller is null"))
    }

    val nonce = generateRawNonce()
    val tracking = when (loginTracking) {
        FacebookLoginTracking.Limited -> FBSDKLoginTrackingLimited
        FacebookLoginTracking.Enabled -> FBSDKLoginTrackingEnabled
    }

    return suspendCancellableCoroutine { continuation ->
        fun resumeOnce(result: Result<FacebookUser>) {
            if (continuation.isActive) continuation.resume(result)
        }

        loginManager.logInFromViewController(
            rootVC,
            FBSDKLoginConfiguration(
                permissions = permissions,
                tracking = tracking,
                nonce = sha256(nonce),
            ),
            completion = { result, error ->
                if (error != null) {
                    currentLogger.log("Facebook Login failed with error: ${error.localizedDescription}")
                    resumeOnce(Result.failure(IllegalStateException(error.localizedDescription)))
                    return@logInFromViewController
                }
                if (result?.isCancelled() == true) {
                    resumeOnce(
                        Result.failure(
                            KMPAuthUserCancelledException("The user cancelled the sign-in flow.")
                        )
                    )
                    return@logInFromViewController
                }

                // Limited Login returns an OIDC authentication token (JWT) plus
                // the raw nonce for Firebase's OAuth provider; classic login
                // returns a Graph-API access token and no nonce.
                val facebookUser = when (loginTracking) {
                    FacebookLoginTracking.Limited -> FacebookUser(
                        accessToken = result?.authenticationToken()?.tokenString() ?: "",
                        nonce = nonce,
                    )

                    FacebookLoginTracking.Enabled -> FacebookUser(
                        accessToken = result?.token()?.tokenString() ?: "",
                        nonce = null,
                    )
                }
                resumeOnce(Result.success(facebookUser))
            }
        )
    }
}

/** Hex-encoded secure random nonce, hashed before being sent to Facebook. */
@OptIn(ExperimentalForeignApi::class, ExperimentalStdlibApi::class)
private fun generateRawNonce(length: Int = 32): String = memScoped {
    val randomBytes = allocArray<UByteVar>(length)
    val errorCode = SecRandomCopyBytes(kSecRandomDefault, length.convert(), randomBytes)
    if (errorCode != errSecSuccess) {
        throw IllegalStateException(
            "Unable to generate random bytes. SecRandomCopyBytes failed with OSStatus $errorCode"
        )
    }
    randomBytes.readBytes(length).toHexString(HexFormat.Default)
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
