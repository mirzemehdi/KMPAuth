package com.mmk.kmpauth.core.auth

/**
 * The backend refused a security-sensitive operation (deleting the account,
 * changing the password or email) because the session's last sign-in is too
 * old.
 *
 * Handle it by obtaining a fresh credential for a provider the user has
 * linked, calling [AuthProviderBackend.reauthenticate], and retrying:
 *
 * ```
 * KMPAuth.deleteAccount().onFailure { error ->
 *     if (error is KMPAuthRecentLoginRequiredException) {
 *         // e.g. ask the email user for their password again:
 *         KMPAuth.reauthenticate(AuthCredential.EmailPassword(email, password))
 *             .onSuccess { KMPAuth.deleteAccount() }
 *     }
 * }
 * ```
 *
 * All backends report this condition with this type, so detection is
 * platform-independent. The original backend exception stays available as
 * [cause].
 */
public class KMPAuthRecentLoginRequiredException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
