package com.mmk.kmpauth.core.auth

/**
 * Provider identifiers used across auth backends. Values follow the
 * Firebase convention; other backends (e.g. Supabase) map them to their
 * own provider names.
 */
public object AuthProviderIds {
    public const val GOOGLE: String = "google.com"
    public const val APPLE: String = "apple.com"
    public const val GITHUB: String = "github.com"
    public const val FACEBOOK: String = "facebook.com"

    /** Email/password provider (Firebase's `EmailAuthProvider` id). */
    public const val EMAIL: String = "password"
}

/**
 * A credential obtained from an identity provider, ready to be exchanged
 * for a session by an [AuthProviderBackend].
 */
public sealed interface AuthCredential {

    /**
     * Identity provider that issued this credential — one of
     * [AuthProviderIds] or a custom OAuth provider id.
     */
    public val providerId: String

    /**
     * Token-based credential: the provider flow already ran on-device and
     * produced an ID token (e.g. Google Sign-In, Sign in with Apple,
     * Facebook Limited Login).
     *
     * @param idToken OpenID Connect ID token issued by the provider.
     * @param accessToken Provider access token, when available.
     * @param rawNonce Unhashed nonce used in the provider request, for
     * backends that verify it (e.g. Apple).
     * @param displayName User's display name when the provider returns it
     * outside the token — Apple hands the full name to the app only on the
     * first authorization, so backends can persist it on the new account.
     */
    public data class IdToken(
        override val providerId: String,
        val idToken: String,
        val accessToken: String? = null,
        val rawNonce: String? = null,
        val displayName: String? = null,
    ) : AuthCredential

    /**
     * Web-flow credential: the backend itself must drive an OAuth web flow
     * for [providerId] (e.g. GitHub via Firebase's OAuthProvider).
     *
     * @param scopes OAuth scopes to request.
     * @param customParameters Extra provider-specific query parameters.
     */
    public data class OAuthWebFlow(
        override val providerId: String,
        val scopes: List<String> = emptyList(),
        val customParameters: Map<String, String> = emptyMap(),
    ) : AuthCredential

    /**
     * Email/password credential, e.g. for sign-in or reauthentication
     * before a security-sensitive operation.
     */
    public data class EmailPassword(
        val email: String,
        val password: String,
    ) : AuthCredential {
        override val providerId: String get() = AuthProviderIds.EMAIL
    }
}
