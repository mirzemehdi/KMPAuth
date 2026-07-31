package com.mmk.kmpauth.core.auth

/**
 * The device-side sign-in provider is unavailable — Google Play services
 * missing, outdated, or disabled, or no component present to handle the
 * sign-in intent.
 *
 * User-fixable: the right response is telling the person to update or
 * enable Google Play services, not a generic "sign-in failed". The
 * original platform exception, when one exists, stays available as
 * [cause].
 */
public class KMPAuthProviderUnavailableException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
