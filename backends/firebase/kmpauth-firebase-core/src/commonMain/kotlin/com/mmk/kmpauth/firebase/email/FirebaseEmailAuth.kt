package com.mmk.kmpauth.firebase.email

import com.mmk.kmpauth.core.auth.KMPAuthUser

/**
 * Link-handling configuration for emails Firebase sends (password reset,
 * passwordless sign-in link). Mirrors Firebase's `ActionCodeSettings`
 * without exposing the underlying SDK type.
 *
 * @param url The link the email lands on. Must be an allowed domain in the
 * Firebase console.
 * @param canHandleCodeInApp true lets the app complete the action from the
 * link directly; required for email-link sign-in.
 * @param iOSBundleId iOS app that may handle the link.
 * @param androidPackageName Android app that may handle the link.
 * @param androidInstallIfNotAvailable Whether to prompt installing the
 * Android app when it is not installed.
 * @param androidMinimumVersion Minimum Android app version able to handle
 * the link.
 * @param linkDomain Custom Firebase Hosting link domain, when configured.
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

/**
 * Email-based Firebase auth operations that don't fit the launch-a-flow
 * shape of [rememberFirebaseEmailSignInState]: password reset,
 * reauthentication and passwordless email-link (magic link) sign-in.
 *
 * The email-link flow spans two app sessions:
 * 1. Call [sendSignInLinkToEmail]; Firebase emails the user a link built
 *    from your [EmailActionCodeSettings]. Persist the email locally (e.g.
 *    in settings storage) — you need it again in step 2.
 * 2. The user opens the link, your app receives it via its deep/universal
 *    link handling. Check it with [isSignInWithEmailLink], then complete
 *    with [signInWithEmailLink] using the persisted email.
 *
 * ```
 * // Step 1 — request the link
 * FirebaseEmailAuth.sendSignInLinkToEmail(
 *     email = email,
 *     actionCodeSettings = EmailActionCodeSettings(
 *         url = "https://example.com/finish-sign-in",
 *         canHandleCodeInApp = true,
 *         iOSBundleId = "com.example.app",
 *         androidPackageName = "com.example.app",
 *     ),
 * )
 *
 * // Step 2 — in your deep link handler
 * if (FirebaseEmailAuth.isSignInWithEmailLink(link)) {
 *     val result = FirebaseEmailAuth.signInWithEmailLink(persistedEmail, link)
 * }
 * ```
 *
 * Enable the "Email link (passwordless sign-in)" method in the Firebase
 * console first. Failures are returned as failed [Result]s, never thrown.
 *
 * Note: on Desktop (JVM) the underlying Firebase SDK does not implement
 * auth yet, and on wasm the SDK has no target — these operations report
 * failed [Result]s there.
 */
public object FirebaseEmailAuth {

    /**
     * Sends a password-reset email for the account registered under
     * [email].
     *
     * @param email Address of the account to reset.
     * @param actionCodeSettings Optional link-handling configuration; when
     * null Firebase uses the console-configured defaults.
     */
    public suspend fun sendPasswordResetEmail(
        email: String,
        actionCodeSettings: EmailActionCodeSettings? = null,
    ): Result<Unit> = firebaseSendPasswordResetEmail(email, actionCodeSettings)

    /**
     * Sends a passwordless sign-in link to [email]. Step 1 of the
     * email-link flow — see the class documentation.
     *
     * @param email Address to send the sign-in link to.
     * @param actionCodeSettings Where the link lands and which apps may
     * handle it; `canHandleCodeInApp` must be true for email-link sign-in.
     */
    public suspend fun sendSignInLinkToEmail(
        email: String,
        actionCodeSettings: EmailActionCodeSettings,
    ): Result<Unit> = firebaseSendSignInLinkToEmail(email, actionCodeSettings)

    /**
     * Returns true when [link] (a deep link your app received) is a
     * Firebase email sign-in link that [signInWithEmailLink] can complete.
     */
    public fun isSignInWithEmailLink(link: String): Boolean =
        firebaseIsSignInWithEmailLink(link)

    /**
     * Completes passwordless sign-in with the [link] the user opened.
     * Step 2 of the email-link flow — see the class documentation.
     *
     * @param email The address the link was sent to (persisted in step 1).
     * @param link The full deep link the app received.
     * @param linkAccount true links the email credential to the currently
     * signed-in Firebase user instead of creating a new session — e.g. to
     * upgrade an anonymous user to a permanent account.
     * @return the signed-in [KMPAuthUser] or the failure.
     */
    public suspend fun signInWithEmailLink(
        email: String,
        link: String,
        linkAccount: Boolean = false,
    ): Result<KMPAuthUser?> = firebaseSignInWithEmailLink(email, link, linkAccount)

    /**
     * Reauthenticates the currently signed-in user with their email/password
     * credential.
     *
     * Firebase requires a recent sign-in before security-sensitive
     * operations — deleting the account, changing the password or email. When
     * such an operation fails with a recent-login-required error, call this
     * with the user's current password and retry:
     *
     * ```
     * FirebaseEmailAuth.reauthenticate(email, currentPassword)
     *     .onSuccess { /* delete the account / update the password */ }
     * ```
     *
     * This is the email convenience over the provider-agnostic
     * [com.mmk.kmpauth.core.auth.AuthProviderBackend.reauthenticate]. For
     * users signed in with Google, Apple or Facebook, rerun the provider
     * flow to get a fresh token and pass it as an
     * [com.mmk.kmpauth.core.auth.AuthCredential.IdToken]:
     *
     * ```
     * // e.g. after rememberGoogleSignInState returns a fresh GoogleUser
     * KMPAuthBackend.require().reauthenticate(
     *     AuthCredential.IdToken(AuthProviderIds.GOOGLE, googleUser.idToken)
     * )
     * ```
     *
     * @param email The signed-in user's email address.
     * @param password The user's current password.
     * @return success, or the failure (no signed-in user, wrong password, ...).
     */
    public suspend fun reauthenticate(
        email: String,
        password: String,
    ): Result<Unit> = firebaseReauthenticate(email, password)
}

internal expect suspend fun firebaseSendPasswordResetEmail(
    email: String,
    actionCodeSettings: EmailActionCodeSettings?,
): Result<Unit>

internal expect suspend fun firebaseSendSignInLinkToEmail(
    email: String,
    actionCodeSettings: EmailActionCodeSettings,
): Result<Unit>

internal expect fun firebaseIsSignInWithEmailLink(link: String): Boolean

internal expect suspend fun firebaseSignInWithEmailLink(
    email: String,
    link: String,
    linkAccount: Boolean,
): Result<KMPAuthUser?>

internal expect suspend fun firebaseReauthenticate(
    email: String,
    password: String,
): Result<Unit>
