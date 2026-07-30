package com.mmk.kmpauth.core.auth

/**
 * Link-handling configuration for emails the auth backend sends (password
 * reset, passwordless sign-in link). Mirrors Firebase's
 * `ActionCodeSettings` without exposing the underlying SDK type; other
 * backends map the fields they support.
 *
 * @param url The link the email lands on. Must be an allowed domain in the
 * backend's console.
 * @param canHandleCodeInApp true lets the app complete the action from the
 * link directly; required for email-link sign-in.
 * @param iOSBundleId iOS app that may handle the link.
 * @param androidPackageName Android app that may handle the link.
 * @param androidInstallIfNotAvailable Whether to prompt installing the
 * Android app when it is not installed.
 * @param androidMinimumVersion Minimum Android app version able to handle
 * the link.
 * @param linkDomain Custom hosting link domain, when configured.
 */
public data class EmailActionCodeSettings(
    val url: String,
    val canHandleCodeInApp: Boolean = false,
    val iOSBundleId: String? = null,
    val androidPackageName: String? = null,
    val androidInstallIfNotAvailable: Boolean = true,
    val androidMinimumVersion: String? = null,
    val linkDomain: String? = null,
)
