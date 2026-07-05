package com.mmk.kmpauth.core.auth

/**
 * Backend-agnostic representation of a signed-in user.
 *
 * Implementations wrap the backend's own user type (for example a Firebase
 * `FirebaseUser` or a Supabase `UserInfo`), which stays reachable through
 * [raw] for callers that need backend-specific data.
 */
public interface KMPAuthUser {
    /** Stable unique identifier assigned by the auth backend. */
    public val uid: String

    /** Email address, when the provider shared one. */
    public val email: String?

    /** Display name, when the provider shared one. */
    public val displayName: String?

    /** Profile photo URL, when the provider shared one. */
    public val photoUrl: String?

    /**
     * Identifier of the identity provider that produced this user,
     * e.g. `"google.com"`, `"apple.com"`, `"github.com"`, `"facebook.com"`.
     */
    public val providerId: String?

    /**
     * The backend's native user object (e.g. `dev.gitlive.firebase.auth.FirebaseUser`).
     * Escape hatch for backend-specific functionality; may be null for fakes.
     */
    public val raw: Any?
}
