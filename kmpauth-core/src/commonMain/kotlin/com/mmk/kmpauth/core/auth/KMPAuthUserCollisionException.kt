package com.mmk.kmpauth.core.auth

/**
 * The credential (or its email) already belongs to a different existing
 * account, so it cannot create a new account or be linked to the current
 * one.
 *
 * The classic case: the app signs every new user in anonymously, then
 * upgrades via `signIn(credential, linkWithCurrentUser = true)` — which
 * fails with this exception when the person has used that Google/Apple/email
 * identity before (e.g. after reinstalling the app). Handle it by signing in
 * WITHOUT linking, which switches to the existing account:
 *
 * ```
 * KMPAuth.signIn(credential, linkWithCurrentUser = true).onFailure { error ->
 *     if (error is KMPAuthUserCollisionException) {
 *         // Returning user: enter their existing account instead. Any data
 *         // the anonymous session accumulated must be migrated by the app.
 *         KMPAuth.signIn(credential)
 *     }
 * }
 * ```
 *
 * All backends report collisions with this type, so detection is
 * platform-independent — no matching on backend-specific exception classes
 * or (possibly empty) error messages. The original backend exception stays
 * available as [cause].
 */
public class KMPAuthUserCollisionException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
