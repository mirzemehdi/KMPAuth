package com.mmk.kmpauth.supabase

import com.mmk.kmpauth.core.KMPAuthInternalApi
import com.mmk.kmpauth.core.auth.AuthCredential
import com.mmk.kmpauth.core.auth.AuthProviderBackend
import com.mmk.kmpauth.core.auth.AuthProviderIds
import com.mmk.kmpauth.core.auth.EmailActionCodeSettings
import com.mmk.kmpauth.core.auth.KMPAuthUser
import com.mmk.kmpauth.core.auth.KMPAuthUserCollisionException
import com.mmk.kmpauth.core.auth.PhoneVerificationUi
import com.mmk.kmpauth.core.runCatchingCancellable
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.exception.AuthErrorCode
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.auth.providers.Apple
import io.github.jan.supabase.auth.providers.Facebook
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.IDTokenProvider
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.providers.builtin.OTP
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.minutes

/**
 * Supabase implementation of [AuthProviderBackend], over a configured
 * supabase-kt [SupabaseClient] with the `Auth` plugin installed. Register it
 * with `KMPAuth.initialize { supabase(...) }` (see
 * [com.mmk.kmpauth.supabase.supabase]) — after that every backend-generic
 * flow (`rememberEmailAuthState`, `rememberAnonymousAuthState`,
 * `rememberGoogleAuthState`, `rememberFacebookAuthState`) and the `KMPAuth`
 * operations run against Supabase.
 *
 * What maps, and how:
 * - Email/password sign-in and sign-up, anonymous sign-in, and password
 *   reset map directly (enable the corresponding providers in the Supabase
 *   dashboard). With email confirmation enabled, [signUp] succeeds with the
 *   created user but no session exists until the address is confirmed.
 * - [AuthCredential.IdToken] from Google, Apple and Facebook Limited Login
 *   is exchanged through Supabase's `id_token` grant. Classic Facebook
 *   access tokens are rejected — Supabase accepts only OIDC tokens.
 * - Email-link (magic link / OTP) sign-in maps onto Supabase's OTP flow;
 *   see [sendSignInLinkToEmail] and [signInWithEmailLink].
 * - Phone sign-in maps onto Supabase's SMS OTP flow ([signInWithPhone]) —
 *   works on every target, including where Firebase phone auth cannot run.
 * - [AuthCredential.OAuthWebFlow] is unsupported: browser flows are driven
 *   by supabase-kt itself (`supabaseClient.auth.signInWith(Github)` etc.),
 *   with its own deep-link handling, not through this backend.
 *
 * The underlying client stays reachable through [supabaseClient] for
 * anything Supabase-specific (identities, MFA, `updateUser`, other plugins).
 * Note that supabase-kt restores a stored session asynchronously at start —
 * [currentUser] may be null until `auth.awaitInitialization()` completes.
 */
/** Id [SupabaseAuthBackend] registers under in the backend registry. */
public const val SUPABASE_BACKEND_ID: String = "supabase"

@OptIn(KMPAuthInternalApi::class)
public class SupabaseAuthBackend(
    public val supabaseClient: SupabaseClient,
) : AuthProviderBackend {

    override val backendId: String get() = SUPABASE_BACKEND_ID

    /**
     * Exchanges [credential] for a Supabase session.
     *
     * With [linkWithCurrentUser] set, an [AuthCredential.IdToken] is linked
     * to the signed-in user via Supabase's identity linking — which requires
     * manual linking to be enabled on the Supabase project. Email/password
     * credentials cannot be linked (Supabase adds an email to an existing
     * account via `auth.updateUser` instead) and report an unsupported
     * failure.
     */
    override suspend fun signIn(
        credential: AuthCredential,
        linkWithCurrentUser: Boolean,
    ): Result<KMPAuthUser> = runCatchingCancellable {
        when (credential) {
            is AuthCredential.EmailPassword -> {
                if (linkWithCurrentUser) throw UnsupportedOperationException(
                    "Supabase cannot link an email/password credential to the " +
                        "current user. Add the email to the signed-in account with " +
                        "supabaseClient.auth.updateUser { email = ...; password = ... } instead."
                )
                supabaseClient.auth.signInWith(Email) {
                    email = credential.email
                    password = credential.password
                }
                requireCurrentUser()
            }

            is AuthCredential.IdToken -> {
                val idTokenProvider = credential.toSupabaseIdTokenProvider()
                if (linkWithCurrentUser) {
                    supabaseClient.auth.linkIdentityWithIdToken(
                        provider = idTokenProvider,
                        idToken = credential.idToken,
                    ) {
                        nonce = credential.rawNonce
                        accessToken = credential.accessToken
                    }
                    SupabaseKMPAuthUser(
                        supabaseClient.auth.retrieveUserForCurrentSession(updateSession = true)
                    )
                } else {
                    supabaseClient.auth.signInWith(IDToken) {
                        idToken = credential.idToken
                        provider = idTokenProvider
                        nonce = credential.rawNonce
                        accessToken = credential.accessToken
                    }
                    requireCurrentUser()
                }
            }

            is AuthCredential.OAuthWebFlow -> {
                if (linkWithCurrentUser) throw UnsupportedOperationException(
                    "Linking a browser OAuth identity goes through supabase-kt " +
                        "directly: supabaseClient.auth.linkIdentity(Github) { ... }."
                )
                val provider = supabaseOAuthProviderOrNull(credential.providerId)
                    ?: throw IllegalArgumentException(
                        "Unknown OAuth provider id '${credential.providerId}'. " +
                            "Use a Firebase-style id (github.com, microsoft.com) " +
                            "or a GoTrue provider name (github, azure, gitlab, ...)."
                    )
                val previousAccessToken = supabaseClient.auth.currentSessionOrNull()?.accessToken
                supabaseClient.auth.signInWith(provider) {
                    scopes.addAll(credential.scopes)
                    queryParams.putAll(credential.customParameters)
                }
                awaitOAuthSession(previousAccessToken)
            }
        }
    }.mappingCollision()

    /**
     * Waits for the browser OAuth round-trip to produce a session. On
     * Desktop supabase-kt's `signInWith` suspends until its built-in
     * localhost callback server receives the redirect, so the session is
     * usually already there; on Android/iOS the flow returns immediately
     * and the session arrives asynchronously through the app's deep link
     * (configure `scheme`/`host` on the Supabase client and forward the
     * deep link per supabase-kt's setup). On web the page redirects away —
     * the session is restored when the app reloads, so no result can be
     * delivered here.
     */
    private suspend fun awaitOAuthSession(previousAccessToken: String?): KMPAuthUser {
        val session = withTimeoutOrNull(OAUTH_FLOW_TIMEOUT) {
            supabaseClient.auth.sessionStatus
                .map { (it as? SessionStatus.Authenticated)?.session }
                .first { it != null && it.accessToken != previousAccessToken }
        } ?: throw IllegalStateException(
            "The OAuth flow did not complete within $OAUTH_FLOW_TIMEOUT. On " +
                "Android/iOS make sure the Supabase deep link is configured " +
                "(scheme/host on the client, plus the platform manifest entry) " +
                "and the redirect URL is allow-listed in the Supabase dashboard."
        )
        return session!!.user?.let(::SupabaseKMPAuthUser)
            ?: SupabaseKMPAuthUser(
                supabaseClient.auth.retrieveUserForCurrentSession(updateSession = true)
            )
    }

    /**
     * Supabase has no Firebase-style recent-login requirement, so
     * reauthentication is a fresh sign-in with [credential]. When the
     * credential resolves to a different account than the one currently
     * signed in, the mismatched session is signed out and a failure
     * returned — reauthentication must never silently switch accounts.
     */
    override suspend fun reauthenticate(credential: AuthCredential): Result<Unit> =
        runCatchingCancellable {
            val currentUid = supabaseClient.auth.currentUserOrNull()?.id
                ?: throw IllegalStateException("No signed-in user to reauthenticate")
            val user = signIn(credential, linkWithCurrentUser = false).getOrThrow()
            if (user.uid != currentUid) {
                supabaseClient.auth.signOut()
                throw IllegalStateException(
                    "Reauthentication credential belongs to a different user; " +
                        "the session has been signed out."
                )
            }
        }

    /**
     * Creates the account via Supabase's email sign-up. With email
     * confirmation enabled on the project the returned user has no active
     * session until the address is confirmed; with auto-confirm the user is
     * signed in directly.
     */
    override suspend fun signUp(email: String, password: String): Result<KMPAuthUser> =
        runCatchingCancellable {
            val user = supabaseClient.auth.signUpWith(Email) {
                this.email = email
                this.password = password
            } ?: supabaseClient.auth.currentUserOrNull()
            user?.let(::SupabaseKMPAuthUser)
                ?: throw IllegalStateException("Supabase returned no user for the sign-up")
        }.mappingCollision()

    /** Requires anonymous sign-ins to be enabled on the Supabase project. */
    override suspend fun signInAnonymously(): Result<KMPAuthUser> = runCatchingCancellable {
        supabaseClient.auth.signInAnonymously()
        requireCurrentUser()
    }

    /**
     * Phone OTP sign-in: sends the SMS via Supabase's OTP flow, obtains the
     * code through [PhoneVerificationUi.awaitVerificationCode] and verifies
     * it. Requires the Phone provider and an SMS sender (Twilio, Vonage,
     * ...) to be configured in the Supabase dashboard. Plain REST — works
     * on every target, no platform verification UI involved.
     *
     * Linking is unsupported: Supabase adds a phone number to the
     * signed-in account via `auth.updateUser` instead.
     */
    override suspend fun signInWithPhone(
        phoneNumber: String,
        verificationUi: PhoneVerificationUi,
        linkWithCurrentUser: Boolean,
    ): Result<KMPAuthUser> = runCatchingCancellable {
        if (linkWithCurrentUser) throw UnsupportedOperationException(
            "Supabase cannot link a phone credential to the current user. Add " +
                "the phone number to the signed-in account with " +
                "supabaseClient.auth.updateUser { phone = ... } instead."
        )
        supabaseClient.auth.signInWith(OTP) {
            phone = phoneNumber
        }
        val code = verificationUi.awaitVerificationCode()
        supabaseClient.auth.verifyPhoneOtp(
            type = OtpType.Phone.SMS,
            phone = phoneNumber,
            token = code,
        )
        requireCurrentUser()
    }

    /**
     * Sends the recovery email via Supabase's `resetPasswordForEmail`. Only
     * [EmailActionCodeSettings.url] maps (as the redirect URL, which must be
     * in the project's allow-list); the app-identity fields are
     * Firebase-dynamic-link concepts with no Supabase counterpart and are
     * ignored.
     */
    override suspend fun sendPasswordResetEmail(
        email: String,
        actionCodeSettings: EmailActionCodeSettings?,
    ): Result<Unit> = runCatchingCancellable {
        supabaseClient.auth.resetPasswordForEmail(
            email = email,
            redirectUrl = actionCodeSettings?.url,
        )
    }

    /**
     * Sends the magic link via Supabase's OTP flow. As with password reset,
     * only [EmailActionCodeSettings.url] maps — it becomes the redirect URL
     * and must be in the project's allow-list.
     */
    override suspend fun sendSignInLinkToEmail(
        email: String,
        actionCodeSettings: EmailActionCodeSettings,
    ): Result<Unit> = runCatchingCancellable {
        supabaseClient.auth.signInWith(OTP, redirectUrl = actionCodeSettings.url) {
            this.email = email
        }
    }

    /**
     * True for the three link shapes Supabase email flows produce:
     * `token_hash` (custom email template, or the default confirmation
     * URL's `token`+`type`), a PKCE `code`, or implicit-flow
     * `access_token`/`refresh_token` fragments.
     */
    override fun isSignInWithEmailLink(link: String): Boolean =
        SupabaseEmailLink.parse(link) != null

    /**
     * Completes the magic-link sign-in for the [link] the app received —
     * `verifyEmailOtp` for token-hash links, `exchangeCodeForSession` for
     * PKCE codes (the code verifier stored by [sendSignInLinkToEmail] on
     * this client is required), or a direct session import for
     * implicit-flow tokens.
     *
     * [email] is not needed by Supabase (the link itself identifies the
     * account) and is accepted only for interface compatibility.
     * [linkAccount] is unsupported: Supabase's verification always signs in
     * the link's account and cannot link the email to the current user —
     * use `supabaseClient.auth.updateUser { email = ... }` to attach an
     * email address to a signed-in (e.g. anonymous) user.
     */
    override suspend fun signInWithEmailLink(
        email: String,
        link: String,
        linkAccount: Boolean,
    ): Result<KMPAuthUser> = runCatchingCancellable {
        if (linkAccount) throw UnsupportedOperationException(
            "Supabase's email-link verification always creates a session for " +
                "the link's account and cannot link it to the current user. Use " +
                "supabaseClient.auth.updateUser { email = ... } to upgrade the " +
                "signed-in user instead."
        )
        val parsed = SupabaseEmailLink.parse(link) ?: throw IllegalArgumentException(
            "Not a Supabase email sign-in link. Expected token_hash (custom " +
                "email template), code (PKCE redirect) or " +
                "access_token/refresh_token (implicit redirect) parameters."
        )
        when (parsed) {
            is SupabaseEmailLink.TokenHash ->
                supabaseClient.auth.verifyEmailOtp(
                    type = parsed.otpType,
                    tokenHash = parsed.tokenHash,
                )

            is SupabaseEmailLink.PkceCode ->
                supabaseClient.auth.exchangeCodeForSession(parsed.code)

            is SupabaseEmailLink.SessionTokens ->
                supabaseClient.auth.importAuthToken(
                    accessToken = parsed.accessToken,
                    refreshToken = parsed.refreshToken,
                    retrieveUser = true,
                )
        }
        requireCurrentUser()
    }

    /**
     * Unsupported: GoTrue exposes user deletion only through the admin API
     * (service-role key), which must never ship in a client. Deletion goes
     * through the app's own backend — typically a Supabase Edge Function
     * calling `auth.admin.deleteUser(uid)` for the authenticated caller.
     */
    override suspend fun deleteAccount(): Result<Unit> = Result.failure(
        UnsupportedOperationException(
            "Supabase has no client-side account deletion - the admin API " +
                "requires the service-role key. Expose a Supabase Edge " +
                "Function that calls auth.admin.deleteUser for the " +
                "authenticated user and invoke it from the app."
        )
    )

    /** Signs out locally (revokes the current session). */
    override suspend fun signOut() {
        supabaseClient.auth.signOut()
    }

    /**
     * The current session's user, or null when signed out. supabase-kt
     * restores persisted sessions asynchronously — right after process
     * start this may still be null; `supabaseClient.auth.awaitInitialization()`
     * suspends until restoration finished.
     */
    override fun currentUser(): KMPAuthUser? =
        supabaseClient.auth.currentUserOrNull()?.let(::SupabaseKMPAuthUser)

    override val currentUserFlow: Flow<KMPAuthUser?>
        get() = supabaseClient.auth.sessionStatus.map { status ->
            (status as? SessionStatus.Authenticated)
                ?.session?.user?.let(::SupabaseKMPAuthUser)
        }

    /**
     * The Supabase access token (a GoTrue JWT verifiable against the
     * project's JWT secret / JWKS). [forceRefresh] refreshes the session
     * first.
     */
    override suspend fun currentUserIdToken(forceRefresh: Boolean): Result<String> =
        runCatchingCancellable {
            if (forceRefresh) supabaseClient.auth.refreshCurrentSession()
            supabaseClient.auth.currentAccessTokenOrNull()
                ?: throw IllegalStateException("No signed-in user to get an ID token for")
        }

    private fun requireCurrentUser(): KMPAuthUser =
        supabaseClient.auth.currentUserOrNull()?.let(::SupabaseKMPAuthUser)
            ?: throw IllegalStateException("Supabase returned no user for the signed-in session")

    /**
     * Surfaces GoTrue already-exists errors as the backend-agnostic
     * [KMPAuthUserCollisionException], matching the Firebase backend.
     */
    private fun <T> Result<T>.mappingCollision(): Result<T> = fold(
        onSuccess = { this },
        onFailure = { error ->
            val isCollision = error is AuthRestException && error.errorCode in setOf(
                AuthErrorCode.EmailExists,
                AuthErrorCode.PhoneExists,
                AuthErrorCode.UserAlreadyExists,
                AuthErrorCode.IdentityAlreadyExists,
            )
            if (isCollision) {
                Result.failure(
                    KMPAuthUserCollisionException(
                        message = error.message?.takeIf { it.isNotBlank() }
                            ?: "This credential is already associated with a different user account.",
                        cause = error,
                    )
                )
            } else this
        },
    )

    private companion object {
        /** Upper bound for the browser OAuth round-trip. */
        val OAUTH_FLOW_TIMEOUT = 5.minutes
    }

    private fun AuthCredential.IdToken.toSupabaseIdTokenProvider(): IDTokenProvider =
        when (providerId) {
            AuthProviderIds.GOOGLE -> Google
            AuthProviderIds.APPLE -> Apple
            AuthProviderIds.FACEBOOK ->
                if (rawNonce != null) Facebook
                else throw UnsupportedOperationException(
                    "Supabase's id_token grant accepts only OIDC tokens; a classic " +
                        "Facebook access token cannot be exchanged. Use Facebook " +
                        "Limited Login (which issues an OIDC id token with a nonce) " +
                        "or Supabase's own OAuth web flow " +
                        "(supabaseClient.auth.signInWith(Facebook))."
                )

            else -> throw UnsupportedOperationException(
                "SupabaseAuthBackend cannot exchange an id token from provider " +
                    "'$providerId'. Supabase supports Google, Apple, Facebook " +
                    "(Limited Login) and Azure id tokens."
            )
        }
}
