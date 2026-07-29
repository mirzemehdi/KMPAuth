package com.mmk.kmpauth.firebase.backend

import android.app.Activity
import com.mmk.kmpauth.core.auth.PhoneVerificationUi
import dev.gitlive.firebase.auth.PhoneVerificationProvider
import java.util.concurrent.TimeUnit

internal actual fun gitLivePhoneVerificationProvider(
    verificationUi: PhoneVerificationUi,
): PhoneVerificationProvider? = object : PhoneVerificationProvider {
    // The Firebase SDK needs the Activity to run its reCAPTCHA/Play
    // Integrity fallback verification UI. rememberPhoneAuthState supplies
    // it via PhoneVerificationUi.platformUiContext.
    override val activity: Activity = requireNotNull(
        verificationUi.platformUiContext as? Activity
    ) { "Phone sign-in requires an Activity context" }
    override val timeout: Long = 60
    override val unit: TimeUnit = TimeUnit.SECONDS
    override fun codeSent(triggerResend: (Unit) -> Unit) = Unit
    override suspend fun getVerificationCode(): String =
        verificationUi.awaitVerificationCode()
}
