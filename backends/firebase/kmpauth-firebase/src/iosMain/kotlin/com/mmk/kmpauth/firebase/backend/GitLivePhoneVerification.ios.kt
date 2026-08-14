package com.mmk.kmpauth.firebase.backend

import swiftPMImport.dev.gitlive.firebase.auth.FIRAuthUIDelegateProtocol
import com.mmk.kmpauth.core.auth.PhoneVerificationUi
import dev.gitlive.firebase.auth.PhoneVerificationProvider
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
internal actual fun gitLivePhoneVerificationProvider(
    verificationUi: PhoneVerificationUi,
): PhoneVerificationProvider? = object : PhoneVerificationProvider {
    // null lets the Firebase SDK present its reCAPTCHA fallback UI over
    // the top view controller.
    override val delegate: FIRAuthUIDelegateProtocol? = null
    override suspend fun getVerificationCode(): String =
        verificationUi.awaitVerificationCode()
}
