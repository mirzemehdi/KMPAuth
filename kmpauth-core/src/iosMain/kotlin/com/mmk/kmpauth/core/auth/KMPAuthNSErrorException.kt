package com.mmk.kmpauth.core.auth

import platform.Foundation.NSError

/**
 * Wraps an [NSError] so the platform error detail survives into `Result`
 * failures instead of being reduced to a localized message: the [domain]
 * and [code] are readable directly, and the full [nsError] (including
 * `userInfo`) stays reachable.
 *
 * iOS-only. Common code sees these failures as plain [Throwable]s with a
 * message that includes the domain and code; iOS code can match precisely:
 *
 * ```
 * result.onFailure { error ->
 *     val nsError = (error as? KMPAuthNSErrorException)?.nsError
 *         ?: (error.cause as? KMPAuthNSErrorException)?.nsError
 * }
 * ```
 *
 * Typed failures ([KMPAuthUserCancelledException], [KMPAuthNetworkException])
 * carry this as their `cause`; unclassified platform errors are surfaced as
 * this type directly.
 */
public class KMPAuthNSErrorException(
    public val nsError: NSError,
) : Exception(
    "${nsError.localizedFailureReason ?: nsError.localizedDescription} " +
        "(${nsError.domain} ${nsError.code})"
) {

    /** [NSError.domain], e.g. `com.apple.AuthenticationServices.AuthorizationError`. */
    public val domain: String? get() = nsError.domain

    /** [NSError.code] within [domain], e.g. 1001 for a cancelled Apple authorization. */
    public val code: Long get() = nsError.code
}
