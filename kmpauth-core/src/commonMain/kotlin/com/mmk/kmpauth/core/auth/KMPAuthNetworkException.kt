package com.mmk.kmpauth.core.auth

/**
 * The operation failed because of a connectivity problem — device offline,
 * request timeout, or the auth service unreachable. Retryable; apps
 * typically show a "check your connection" message.
 *
 * Mapped from the platform signals where they are reliable (Play services
 * network status codes and Firebase's network exception on Android,
 * `NSURLErrorDomain` on iOS/Apple platforms). Failures the platform does
 * not clearly attribute to connectivity keep their original type, so
 * absence of this exception does not prove the network was fine. The
 * original platform exception, when one exists, stays available as [cause].
 */
public class KMPAuthNetworkException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
