package com.mmk.kmpauth.google

import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.mmk.kmpauth.core.auth.KMPAuthNetworkException
import com.mmk.kmpauth.core.auth.KMPAuthUserCancelledException

// GoogleSignInStatusCodes.SIGN_IN_CANCELLED - referenced by value to avoid
// depending on the deprecated legacy artifact's constant class here.
private const val LEGACY_SIGN_IN_CANCELLED = 12501

/**
 * Maps the platform sign-in failures with a reliable classification onto
 * KMPAuth's typed exceptions; anything else passes through unchanged.
 */
internal fun Throwable.asKMPAuthError(): Throwable = when {
    this is GetCredentialCancellationException -> KMPAuthUserCancelledException(
        message = "The user cancelled the sign-in flow.",
        cause = this,
    )

    this is ApiException && (
        statusCode == CommonStatusCodes.CANCELED || statusCode == LEGACY_SIGN_IN_CANCELLED
        ) -> KMPAuthUserCancelledException(
        message = "The user cancelled the sign-in flow.",
        cause = this,
    )

    this is ApiException && (
        statusCode == CommonStatusCodes.NETWORK_ERROR || statusCode == CommonStatusCodes.TIMEOUT
        ) -> KMPAuthNetworkException(
        message = "Google sign-in failed because of a network problem.",
        cause = this,
    )

    else -> this
}
