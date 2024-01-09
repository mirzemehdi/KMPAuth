package com.mmk.kmpauth.firebase.core

import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import dev.gitlive.firebase.auth.FirebaseUser
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UByteVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.refTo
import kotlinx.cinterop.usePinned
import platform.AuthenticationServices.ASAuthorization
import platform.AuthenticationServices.ASAuthorizationAppleIDProvider
import platform.AuthenticationServices.ASAuthorizationController
import platform.AuthenticationServices.ASAuthorizationControllerDelegateProtocol
import platform.AuthenticationServices.ASAuthorizationControllerPresentationContextProvidingProtocol
import platform.AuthenticationServices.ASAuthorizationPasswordProvider
import platform.AuthenticationServices.ASAuthorizationScopeEmail
import platform.AuthenticationServices.ASAuthorizationScopeFullName
import platform.AuthenticationServices.ASPresentationAnchor
import platform.CoreCrypto.CC_SHA256
import platform.CoreCrypto.CC_SHA256_DIGEST_LENGTH
import platform.Foundation.NSError
import platform.Security.SecRandomCopyBytes
import platform.Security.errSecSuccess
import platform.Security.kSecRandomDefault
import platform.UIKit.UIApplication
import platform.darwin.NSObject

@Composable
public actual fun SignInWithAppleButton(
    modifier: Modifier,
    onResult: (Result<FirebaseUser?>) -> Unit,
) {

    val authorizationController: ASAuthorizationController by remember {
        val nonce = randomNonceString()
        val appleIdProvider = ASAuthorizationAppleIDProvider()
        val request = appleIdProvider.createRequest()
        val passwordRequest = ASAuthorizationPasswordProvider().createRequest()
        request.requestedScopes = listOf(ASAuthorizationScopeFullName, ASAuthorizationScopeEmail)
        request.nonce = sha256(nonce)
        val controller = ASAuthorizationController(listOf(request, passwordRequest))
        controller.setPresentationContextProvider(PresentationContextProvider())
        controller.setDelegate(ASAuthorizationControllerDelegate())
        mutableStateOf(controller)
    }

    Button(onClick = { authorizationController.performRequests() }) {
        Text("Sign-In with Apple (Firebase)")
    }
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
    override fun presentationAnchorForAuthorizationController(controller: ASAuthorizationController): ASPresentationAnchor? {
        println("presentationAnchorForAuthorizationController is called")
        val rootViewController =
            UIApplication.sharedApplication.keyWindow?.rootViewController

        return rootViewController?.view?.window
    }
}

private class ASAuthorizationControllerDelegate :
    ASAuthorizationControllerDelegateProtocol, NSObject() {


    override fun authorizationController(
        controller: ASAuthorizationController,
        didCompleteWithAuthorization: ASAuthorization,
    ) {
//        val authorization = didCompleteWithAuthorization
        println("AppleIdToken: didCompleteWithAuthorization is called")

//        val appleIDCredential = authorization.credential as? ASAuthorizationAppleIDCredential
//
//        val appleIdToken = appleIDCredential?.identityToken
//        println("AppleIdToken: $appleIdToken")

    }

    override fun authorizationController(
        controller: ASAuthorizationController,
        didCompleteWithError: NSError,
    ) {
        println("AppleIdToken: Error occurred with Apple: $didCompleteWithError")

//        println("Error occurred with Apple Sign-In: $didCompleteWithError")
    }


}


