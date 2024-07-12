package com.mmk.kmpauth.firebase.apple

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import androidx.compose.ui.unit.dp
import cocoapods.FirebaseAuth.FIRAuth
import cocoapods.FirebaseAuth.FIRAuthDataResult
import cocoapods.FirebaseAuth.FIROAuthProvider
import com.mmk.kmpauth.core.UiContainerScope
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.auth
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCAction
import kotlinx.cinterop.UByteVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.refTo
import kotlinx.cinterop.usePinned
import platform.AuthenticationServices.ASAuthorization
import platform.AuthenticationServices.ASAuthorizationAppleIDButton
import platform.AuthenticationServices.ASAuthorizationAppleIDCredential
import platform.AuthenticationServices.ASAuthorizationAppleIDProvider
import platform.AuthenticationServices.ASAuthorizationController
import platform.AuthenticationServices.ASAuthorizationControllerDelegateProtocol
import platform.AuthenticationServices.ASAuthorizationControllerPresentationContextProvidingProtocol
import platform.AuthenticationServices.ASAuthorizationScopeEmail
import platform.AuthenticationServices.ASAuthorizationScopeFullName
import platform.AuthenticationServices.ASPresentationAnchor
import platform.CoreCrypto.CC_SHA256
import platform.CoreCrypto.CC_SHA256_DIGEST_LENGTH
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSError
import platform.Foundation.NSSelectorFromString
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Security.SecRandomCopyBytes
import platform.Security.errSecSuccess
import platform.Security.kSecRandomDefault
import platform.UIKit.UIApplication
import platform.UIKit.UIControlEventTouchUpInside
import platform.UIKit.UIStackView
import platform.darwin.NSObject

private var currentNonce: String? = null

/**
 * AppleButton Ui Container Composable that handles all sign-in functionality for Apple.
 * Child of this Composable can be any view or Composable function.
 * You need to call [UiContainerScope.onClick] function on your child view's click function.
 *
 * [onResult] callback will return [Result] with [FirebaseUser] type.
 * @param requestScopes list of request scopes type of [AppleSignInRequestScope].
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
@Composable
public actual fun AppleButtonUiContainer(
    modifier: Modifier,
    requestScopes: List<AppleSignInRequestScope>,
    onResult: (Result<FirebaseUser?>) -> Unit,
    linkAccount: Boolean,
    content: @Composable UiContainerScope.() -> Unit,
) {
    val updatedOnResultFunc by rememberUpdatedState(onResult)
    val presentationContextProvider = PresentationContextProvider()
    val asAuthorizationControllerDelegate =
        ASAuthorizationControllerDelegate(linkAccount, updatedOnResultFunc)

    val uiContainerScope = remember {
        object : UiContainerScope {
            override fun onClick() {
                signIn(
                    requestScopes = requestScopes,
                    authorizationController = asAuthorizationControllerDelegate,
                    presentationContextProvider = presentationContextProvider,
                )
            }

        }
    }
    Box(modifier = modifier) { uiContainerScope.content() }

}

@Deprecated(
    "Use AppleButtonUiContainer with linkAccount parameter",
    ReplaceWith("AppleButtonUiContainer(modifier, requestScopes, onResult, false, content)"),
    DeprecationLevel.WARNING
)
@Composable
public actual fun AppleButtonUiContainer(
    modifier: Modifier,
    requestScopes: List<AppleSignInRequestScope>,
    onResult: (Result<FirebaseUser?>) -> Unit,
    content: @Composable UiContainerScope.() -> Unit,
) {
    AppleButtonUiContainer(modifier, requestScopes, onResult, false, content)
}

private fun signIn(
    requestScopes: List<AppleSignInRequestScope>,
    authorizationController: ASAuthorizationControllerDelegate,
    presentationContextProvider: PresentationContextProvider,
) {
    val appleIdProviderRequest = ASAuthorizationAppleIDProvider().createRequest()
    appleIdProviderRequest.requestedScopes = requestScopes.map {
        when (it) {
            AppleSignInRequestScope.Email -> ASAuthorizationScopeEmail
            AppleSignInRequestScope.FullName -> ASAuthorizationScopeFullName
        }
    }
    val nonce = randomNonceString()
    currentNonce = nonce
    appleIdProviderRequest.nonce = sha256(nonce)
    val requests = listOf(appleIdProviderRequest)
    val controller = ASAuthorizationController(requests)
    controller.delegate = authorizationController
    controller.presentationContextProvider = presentationContextProvider
    controller.performRequests()
}


private fun randomNonceString(length: Int = 32): String {
    require(length > 0) { "Length must be greater than 0" }
    val randomBytes = iosSecureRandomBytes(length)
    val charset = "0123456789ABCDEFGHIJKLMNOPQRSTUVXYZabcdefghijklmnopqrstuvwxyz-._"
    val nonce = randomBytes.map { byte ->
        charset[(byte.toInt() and 0xFF) % charset.length]
    }.joinToString("")

    return nonce

}


@OptIn(ExperimentalForeignApi::class)
private fun iosSecureRandomBytes(length: Int): ByteArray {
    require(length > 0) { "Length must be greater than 0" }
    return memScoped {
        val randomBytes = allocArray<UByteVar>(length)
        val errorCode = SecRandomCopyBytes(kSecRandomDefault, length.convert(), randomBytes)
        if (errorCode != errSecSuccess) {
            throw RuntimeException("Unable to generate random bytes. SecRandomCopyBytes failed with OSStatus $errorCode")
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

private class PresentationContextProvider :
    ASAuthorizationControllerPresentationContextProvidingProtocol, NSObject() {

    override fun presentationAnchorForAuthorizationController(controller: ASAuthorizationController): ASPresentationAnchor {
        val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
        return rootViewController?.view?.window
    }
}

private class ASAuthorizationControllerDelegate(
    private val linkAccount: Boolean,
    private val onResult: (Result<FirebaseUser?>) -> Unit
) :
    ASAuthorizationControllerDelegateProtocol, NSObject() {

    @OptIn(BetaInteropApi::class, ExperimentalForeignApi::class)
    override fun authorizationController(
        controller: ASAuthorizationController,
        didCompleteWithAuthorization: ASAuthorization,
    ) {
        println("AppleSignIn: authorizationController success function is called")
        val authorization = didCompleteWithAuthorization

        val appleIDCredential = authorization.credential as? ASAuthorizationAppleIDCredential
        if (currentNonce == null) {
            onResult(Result.failure(IllegalStateException("Invalid state: A login callback was received, but no login request was sent.")))
            return
        }
        val appleIdToken = appleIDCredential?.identityToken
        if (appleIdToken == null) {
            onResult(Result.failure(IllegalStateException("Unable to fetch identity token")))
            return
        }
        val idTokenString = NSString.create(appleIdToken, NSUTF8StringEncoding)?.toString()
        if (idTokenString == null) {
            onResult(Result.failure(IllegalStateException("Unable to serialize token string from data")))
            return
        }

        // Initialize a Firebase credential, including the user's full name.
        val credential = FIROAuthProvider.appleCredentialWithIDToken(
            idTokenString, currentNonce, appleIDCredential.fullName
        )

        val currentUser = FIRAuth.auth().currentUser

        val handleResult: (FIRAuthDataResult?, NSError?) -> Unit = { firAuthDataResult, nsError ->
            if (nsError != null || firAuthDataResult == null) {
                onResult(Result.failure(IllegalStateException(nsError?.localizedFailureReason)))
            } else {
                onResult(Result.success(Firebase.auth.currentUser))
            }
        }

        if (linkAccount && currentUser != null) {
            currentUser.linkWithCredential(credential, handleResult)
        } else {
            FIRAuth.auth().signInWithCredential(credential, handleResult)
        }
    }

    override fun authorizationController(
        controller: ASAuthorizationController,
        didCompleteWithError: NSError,
    ) {
        onResult(Result.failure(IllegalStateException(didCompleteWithError.localizedFailureReason)))
    }


}


