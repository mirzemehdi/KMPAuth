package com.mmk.kmpauth.core.auth

/**
 * No usable provider account is available on the device — e.g. Google
 * sign-in on a device with no Google account.
 *
 * User-fixable: the right response is telling the person to add an
 * account in the device settings, not a generic "sign-in failed".
 * The original platform exception, when one exists, stays available as
 * [cause].
 */
public class KMPAuthNoAccountAvailableException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
