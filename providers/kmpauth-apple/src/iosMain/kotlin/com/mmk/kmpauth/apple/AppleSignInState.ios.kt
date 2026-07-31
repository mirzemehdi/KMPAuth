package com.mmk.kmpauth.apple

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import com.mmk.kmpauth.core.KMPAuthInternalApi
import com.mmk.kmpauth.core.LaunchingSignInState
import com.mmk.kmpauth.core.SignInState
import com.mmk.kmpauth.core.auth.KMPAuthNSErrorException
import com.mmk.kmpauth.core.auth.KMPAuthNetworkException
import com.mmk.kmpauth.core.auth.KMPAuthUserCancelledException
import com.mmk.kmpauth.core.logger.currentLogger
import kotlinx.cinterop.BetaInteropApi
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
import platform.AuthenticationServices.ASAuthorization
import platform.AuthenticationServices.ASAuthorizationAppleIDCredential
import platform.AuthenticationServices.ASAuthorizationAppleIDProvider
import platform.AuthenticationServices.ASAuthorizationController
import platform.AuthenticationServices.ASAuthorizationControllerDelegateProtocol
import platform.AuthenticationServices.ASAuthorizationControllerPresentationContextProvidingProtocol
import platform.AuthenticationServices.ASAuthorizationErrorCanceled
import platform.AuthenticationServices.ASAuthorizationErrorDomain
import platform.AuthenticationServices.ASAuthorizationScopeEmail
import platform.AuthenticationServices.ASAuthorizationScopeFullName
import platform.AuthenticationServices.ASPresentationAnchor
import platform.CoreCrypto.CC_SHA256
import platform.CoreCrypto.CC_SHA256_DIGEST_LENGTH
import platform.Foundation.NSError
import platform.Foundation.NSPersonNameComponents
import platform.Foundation.NSString
import platform.Foundation.NSURLErrorDomain
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Security.SecRandomCopyBytes
import platform.Security.errSecSuccess
import platform.Security.kSecRandomDefault
import platform.UIKit.UIApplication
import platform.darwin.NSObject
import kotlin.coroutines.resume

/**
 * Result of a native Sign in with Apple authorization, including the platform
 * credential pieces that backend integrations need.
 *
 * Exposed for KMPAuth's own Firebase modules, which must hand Apple's native
 * [fullNameComponents] to `FIROAuthProvider` so Firebase can populate the
 * display name on first sign-in. Application code should use
 * [rememberAppleSignInState] and [AppleUser] instead.
 */
@KMPAuthInternalApi
public class AppleNativeCredential internal constructor(
    public val idToken: String,
    public val rawNonce: String,
    public val fullNameComponents: NSPersonNameComponents?,
    public val user: AppleUser,
)

@OptIn(KMPAuthInternalApi::class)
@Composable
public actual fun rememberAppleSignInState(
    requestScopes: List<AppleSignInRequestScope>,
    onResult: (Result<AppleUser>) -> Unit,
): SignInState {
    val scope = rememberCoroutineScope()
    val currentRequestScopes by rememberUpdatedState(requestScopes)
    val currentOnResult by rememberUpdatedState(onResult)

    return remember {
        LaunchingSignInState(scope) {
            currentOnResult(performAppleSignIn(currentRequestScopes).map { it.user })
        }
    }
}

/**
 * Runs the native Apple authorization flow and returns its credential.
 *
 * Exposed for KMPAuth's Firebase modules; application code should use
 * [rememberAppleSignInState].
 */
@KMPAuthInternalApi
public suspend fun performAppleSignIn(
    requestScopes: List<AppleSignInRequestScope>,
): Result<AppleNativeCredential> = suspendCancellableCoroutine { continuation ->
    val nonce = randomNonceString()
    val presentationContextProvider = PresentationContextProvider()
    val delegate = AppleAuthorizationDelegate(rawNonce = nonce) { result ->
        // Release the strong references held for the in-flight request.
        inFlightAuthorizationDelegate = null
        inFlightPresentationContextProvider = null
        if (continuation.isActive) continuation.resume(result)
    }
    // ASAuthorizationController keeps only weak references to its delegate and
    // presentation context provider, so hold strong references while a sign-in
    // flow is in flight.
    inFlightAuthorizationDelegate = delegate
    inFlightPresentationContextProvider = presentationContextProvider

    val request = ASAuthorizationAppleIDProvider().createRequest()
    request.requestedScopes = requestScopes.map {
        when (it) {
            AppleSignInRequestScope.Email -> ASAuthorizationScopeEmail
            AppleSignInRequestScope.FullName -> ASAuthorizationScopeFullName
        }
    }
    // Apple embeds the hashed nonce in the identity token; the raw value is
    // handed back so backends can verify the claim.
    request.nonce = sha256(nonce)

    val controller = ASAuthorizationController(listOf(request))
    controller.delegate = delegate
    controller.presentationContextProvider = presentationContextProvider
    controller.performRequests()
}

private var inFlightAuthorizationDelegate: AppleAuthorizationDelegate? = null
private var inFlightPresentationContextProvider: PresentationContextProvider? = null

private class PresentationContextProvider :
    ASAuthorizationControllerPresentationContextProvidingProtocol, NSObject() {

    override fun presentationAnchorForAuthorizationController(
        controller: ASAuthorizationController,
    ): ASPresentationAnchor {
        val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
        return rootViewController?.view?.window
    }
}

@OptIn(KMPAuthInternalApi::class)
private class AppleAuthorizationDelegate(
    private val rawNonce: String,
    private val onResult: (Result<AppleNativeCredential>) -> Unit,
) : ASAuthorizationControllerDelegateProtocol, NSObject() {

    @OptIn(BetaInteropApi::class, ExperimentalForeignApi::class)
    override fun authorizationController(
        controller: ASAuthorizationController,
        didCompleteWithAuthorization: ASAuthorization,
    ) {
        currentLogger.log("AppleSignIn: authorization completed")
        val appleIdCredential =
            didCompleteWithAuthorization.credential as? ASAuthorizationAppleIDCredential
        if (appleIdCredential == null) {
            onResult(Result.failure(IllegalStateException("Unexpected Apple credential type")))
            return
        }

        val identityToken = appleIdCredential.identityToken
        if (identityToken == null) {
            onResult(Result.failure(IllegalStateException("Unable to fetch identity token")))
            return
        }
        val idTokenString = NSString.create(identityToken, NSUTF8StringEncoding)?.toString()
        if (idTokenString == null) {
            onResult(Result.failure(IllegalStateException("Unable to serialize token string from data")))
            return
        }

        val fullNameComponents = appleIdCredential.fullName
        val user = AppleUser(
            idToken = idTokenString,
            nonce = rawNonce,
            userId = appleIdCredential.user,
            email = appleIdCredential.email,
            fullName = fullNameComponents?.formatted(),
        )
        onResult(
            Result.success(
                AppleNativeCredential(
                    idToken = idTokenString,
                    rawNonce = rawNonce,
                    fullNameComponents = fullNameComponents,
                    user = user,
                )
            )
        )
    }

    override fun authorizationController(
        controller: ASAuthorizationController,
        didCompleteWithError: NSError,
    ) {
        onResult(Result.failure(didCompleteWithError.asAppleSignInError()))
    }
}

/**
 * Classifies the AuthenticationServices failure. The full [NSError] stays
 * reachable through [KMPAuthNSErrorException] — as the failure itself when
 * unclassified, as the `cause` of the typed exceptions otherwise.
 */
private fun NSError.asAppleSignInError(): Throwable {
    val wrapped = KMPAuthNSErrorException(this)
    return when {
        domain == ASAuthorizationErrorDomain && code == ASAuthorizationErrorCanceled ->
            KMPAuthUserCancelledException(
                message = "The user cancelled the sign-in flow. ($domain $code)",
                cause = wrapped,
            )

        domain == NSURLErrorDomain ->
            KMPAuthNetworkException(
                message = "Apple sign-in failed because of a network problem: ${wrapped.message}",
                cause = wrapped,
            )

        else -> wrapped
    }
}

/** "Given Family", or null when Apple shared neither part. */
private fun NSPersonNameComponents.formatted(): String? =
    listOfNotNull(givenName, familyName)
        .filter { it.isNotBlank() }
        .joinToString(" ")
        .ifBlank { null }

private fun randomNonceString(length: Int = 32): String {
    require(length > 0) { "Length must be greater than 0" }
    val randomBytes = iosSecureRandomBytes(length)
    val charset = "0123456789ABCDEFGHIJKLMNOPQRSTUVXYZabcdefghijklmnopqrstuvwxyz-._"
    return randomBytes.map { byte -> charset[(byte.toInt() and 0xFF) % charset.length] }
        .joinToString("")
}

@OptIn(ExperimentalForeignApi::class)
private fun iosSecureRandomBytes(length: Int): ByteArray {
    require(length > 0) { "Length must be greater than 0" }
    return memScoped {
        val randomBytes = allocArray<UByteVar>(length)
        val errorCode = SecRandomCopyBytes(kSecRandomDefault, length.convert(), randomBytes)
        if (errorCode != errSecSuccess) {
            throw RuntimeException(
                "Unable to generate random bytes. SecRandomCopyBytes failed with OSStatus $errorCode"
            )
        }
        randomBytes.readBytes(length)
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
