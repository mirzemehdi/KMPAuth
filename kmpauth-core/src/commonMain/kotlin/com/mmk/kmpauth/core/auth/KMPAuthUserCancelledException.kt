package com.mmk.kmpauth.core.auth

/**
 * The user cancelled the sign-in flow — dismissed the account picker,
 * closed the provider sheet or popup, or pressed back.
 *
 * By far the most common non-success outcome, and the one an app usually
 * handles by staying silent instead of showing an error:
 *
 * ```
 * result.onFailure { error ->
 *     if (error !is KMPAuthUserCancelledException) showError(error)
 * }
 * ```
 *
 * Every provider maps its platform cancellation signal to this type
 * (Credential Manager and Play services cancellations on Android,
 * `ASAuthorizationError.canceled` / the GIDSignIn cancel code on iOS, the
 * closed Google popup on web, the Facebook SDK's cancel callback), so one
 * `is`-check works on every platform. The original platform exception, when
 * one exists, stays available as [cause].
 */
public class KMPAuthUserCancelledException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
