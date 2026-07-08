package com.mmk.kmpauth.facebook

/**
 * Facebook login tracking mode, controlling which token the flow returns.
 *
 * Facebook offers two login modes with different privacy characteristics and
 * different result tokens:
 *
 * - [Limited] — privacy-friendly "Limited Login". On iOS it does not require
 *   the App Tracking Transparency prompt. The flow returns an OpenID Connect
 *   **authentication token (a JWT)** together with a nonce; both are exposed
 *   through [FacebookUser] (`accessToken` holds the JWT, `nonce` the raw
 *   nonce). With Firebase this is exchanged through the generic OAuth/OIDC
 *   provider. This is the default.
 * - [Enabled] — classic login. The flow returns a real Facebook **access
 *   token** in [FacebookUser.accessToken] (usable against the Graph API and
 *   Firebase's `FacebookAuthProvider`). On iOS this counts as tracking and
 *   requires you to handle App Tracking Transparency.
 *
 * The mode determines the token type on **every** platform, so the behavior is
 * consistent across Android and iOS. Pick [Enabled] when your backend needs a
 * Graph-API access token; keep [Limited] for the privacy-friendly default.
 */
public enum class FacebookLoginTracking {
    /** Privacy-friendly Limited Login: returns an OIDC JWT + nonce. Default. */
    Limited,

    /** Classic login: returns a Graph-API access token (tracking on iOS). */
    Enabled,
}
