package com.mmk.kmpauth.firebase.backend

import com.mmk.kmpauth.core.auth.PhoneVerificationUi
import dev.gitlive.firebase.auth.PhoneVerificationProvider

internal actual fun gitLivePhoneVerificationProvider(
    verificationUi: PhoneVerificationUi,
): PhoneVerificationProvider? = null
