package com.mmk.kmpauth.google

import android.content.ActivityNotFoundException
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.mmk.kmpauth.core.KMPAuthInternalApi
import com.mmk.kmpauth.core.auth.KMPAuthNetworkException
import com.mmk.kmpauth.core.auth.KMPAuthNoAccountAvailableException
import com.mmk.kmpauth.core.auth.KMPAuthProviderUnavailableException
import com.mmk.kmpauth.core.auth.KMPAuthUserCancelledException
import com.mmk.kmpauth.core.di.applicationContext

/**
 * Maps the platform sign-in failures with a reliable, documented
 * classification onto KMPAuth's typed exceptions; anything else passes
 * through unchanged.
 *
 * The mapping must live here rather than in apps: the Credential Manager
 * exceptions that signal these conditions are consumed by the legacy
 * fallback inside the library, so by the time `onFailure` runs only the
 * legacy path's [ApiException] remains.
 */
internal fun Throwable.asKMPAuthError(): Throwable = when {
    this is GetCredentialCancellationException -> KMPAuthUserCancelledException(
        message = "The user cancelled the sign-in flow.",
        cause = this,
    )

    this is ActivityNotFoundException -> KMPAuthProviderUnavailableException(
        message = "No component on this device can handle Google sign-in - " +
            "Google Play services is missing or disabled.",
        cause = this,
    )

    // Provider codes like 28404 are ambiguous (also config errors); real
    // connectivity at failure time is the reliable offline signal (#232).
    this is GetCredentialException && isDeviceOffline() -> KMPAuthNetworkException(
        message = "Google sign-in failed while the device was offline - " +
            "check the connection and retry.",
        cause = this,
    )

    this is ApiException -> when (statusCode) {
        CommonStatusCodes.CANCELED,
        GoogleSignInStatusCodes.SIGN_IN_CANCELLED,
        -> KMPAuthUserCancelledException(
            message = "The user cancelled the sign-in flow.",
            cause = this,
        )

        CommonStatusCodes.NETWORK_ERROR,
        CommonStatusCodes.TIMEOUT,
        -> KMPAuthNetworkException(
            message = "Google sign-in failed because of a network problem.",
            cause = this,
        )

        CommonStatusCodes.SIGN_IN_REQUIRED,
        GoogleSignInStatusCodes.SIGN_IN_FAILED,
        -> KMPAuthNoAccountAvailableException(
            message = "No usable Google account on this device - add one in " +
                "the device settings and retry.",
            cause = this,
        )

        ConnectionResult.SERVICE_MISSING,
        ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED,
        ConnectionResult.SERVICE_DISABLED,
        CommonStatusCodes.API_NOT_CONNECTED,
        -> KMPAuthProviderUnavailableException(
            message = "Google Play services is unavailable, disabled or out " +
                "of date on this device - update or enable it and retry.",
            cause = this,
        )

        else -> this
    }

    else -> this
}

/**
 * True only when the device verifiably has no internet-capable network;
 * any doubt counts as online so the original exception passes through.
 */
@OptIn(KMPAuthInternalApi::class)
private fun isDeviceOffline(): Boolean = try {
    val manager =
        applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    when (val network = manager?.activeNetwork) {
        null -> manager != null
        else -> manager.getNetworkCapabilities(network)
            ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == false
    }
} catch (_: Exception) {
    false
}
