package com.mmk.kmpauth.apple

/**
 * A user returned by native Sign in with Apple.
 *
 * Send [idToken] (and [nonce], when your backend verifies it) to your server;
 * the token is a signed JWT that can be validated against Apple's public keys,
 * so no client secret is needed on the client.
 *
 * @param idToken Apple's identity token (JWT). Contains the user identifier,
 * and the email when the user agreed to share it.
 * @param nonce The **raw** (unhashed) nonce that was sent - hashed - with the
 * authorization request. Backends that verify the token's `nonce` claim need
 * this value; the claim holds its SHA-256 hash.
 * @param userId Apple's stable user identifier for this app.
 * @param email User's email, when requested and shared. **Apple only returns
 * this on the very first authorization** - persist it server-side; later
 * sign-ins return null.
 * @param fullName User's display name, when requested and shared. Subject to
 * the same first-authorization-only rule as [email].
 */
public data class AppleUser(
    val idToken: String,
    val nonce: String,
    val userId: String? = null,
    val email: String? = null,
    val fullName: String? = null,
)
