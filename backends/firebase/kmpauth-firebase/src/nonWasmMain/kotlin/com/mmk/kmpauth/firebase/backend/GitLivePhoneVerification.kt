package com.mmk.kmpauth.firebase.backend

import com.mmk.kmpauth.core.auth.PhoneVerificationUi
import dev.gitlive.firebase.auth.PhoneVerificationProvider

/**
 * Builds the platform's GitLive [PhoneVerificationProvider] around the
 * caller's [PhoneVerificationUi], or returns null on platforms where the
 * Firebase SDK cannot run phone verification (JS needs a reCAPTCHA
 * verifier KMPAuth does not provide; the JVM uses the REST engine, which
 * has no phone support either).
 */
internal expect fun gitLivePhoneVerificationProvider(
    verificationUi: PhoneVerificationUi,
): PhoneVerificationProvider?
