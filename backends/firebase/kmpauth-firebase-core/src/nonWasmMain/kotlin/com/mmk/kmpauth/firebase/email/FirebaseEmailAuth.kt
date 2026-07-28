package com.mmk.kmpauth.firebase.email

import com.mmk.kmpauth.core.KMPAuthInternalApi
import com.mmk.kmpauth.core.auth.KMPAuthBackend
import com.mmk.kmpauth.core.runCatchingCancellable
import com.mmk.kmpauth.firebase.backend.FirebaseAuthBackend
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.ActionCodeSettings
import dev.gitlive.firebase.auth.EmailAuthProvider
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.auth

/**
 * Email-based Firebase auth operations that don't fit the launch-a-flow
 * shape of [rememberFirebaseEmailSignInState]: password reset and
 * passwordless email-link (magic link) sign-in.
 *
 * The email-link flow spans two app sessions:
 * 1. Call [sendSignInLinkToEmail]; Firebase emails the user a link built
 *    from your [ActionCodeSettings]. Persist the email locally (e.g. in
 *    settings storage) — you need it again in step 2.
 * 2. The user opens the link, your app receives it via its deep/universal
 *    link handling. Check it with [isSignInWithEmailLink], then complete
 *    with [signInWithEmailLink] using the persisted email.
 *
 * ```
 * // Step 1 — request the link
 * FirebaseEmailAuth.sendSignInLinkToEmail(
 *     email = email,
 *     actionCodeSettings = ActionCodeSettings(
 *         url = "https://example.com/finish-sign-in",
 *         canHandleCodeInApp = true,
 *         iOSBundleId = "com.example.app",
 *         androidPackageName = AndroidPackageName("com.example.app"),
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
 * auth yet, so these operations report failed [Result]s there.
 */
@OptIn(KMPAuthInternalApi::class)
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
        actionCodeSettings: ActionCodeSettings? = null,
    ): Result<Unit> = runCatchingCancellable {
        Firebase.auth.sendPasswordResetEmail(email, actionCodeSettings)
    }

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
        actionCodeSettings: ActionCodeSettings,
    ): Result<Unit> = runCatchingCancellable {
        Firebase.auth.sendSignInLinkToEmail(email, actionCodeSettings)
    }

    /**
     * Returns true when [link] (a deep link your app received) is a
     * Firebase email sign-in link that [signInWithEmailLink] can complete.
     */
    public fun isSignInWithEmailLink(link: String): Boolean =
        Firebase.auth.isSignInWithEmailLink(link)

    /**
     * Completes passwordless sign-in with the [link] the user opened.
     * Step 2 of the email-link flow — see the class documentation.
     *
     * @param email The address the link was sent to (persisted in step 1).
     * @param link The full deep link the app received.
     * @param linkAccount true links the email credential to the currently
     * signed-in Firebase user instead of creating a new session — e.g. to
     * upgrade an anonymous user to a permanent account.
     * @return the signed-in [FirebaseUser] or the failure.
     */
    public suspend fun signInWithEmailLink(
        email: String,
        link: String,
        linkAccount: Boolean = false,
    ): Result<FirebaseUser?> = runCatchingCancellable {
        // Lazy default registration: no-op when the app already registered
        // a backend at startup (first registration wins).
        KMPAuthBackend.register(FirebaseAuthBackend)
        val auth = Firebase.auth
        val currentUser = auth.currentUser
        val result = if (linkAccount && currentUser != null) {
            currentUser.linkWithCredential(EmailAuthProvider.credentialWithLink(email, link))
        } else {
            auth.signInWithEmailLink(email, link)
        }
        result.user
    }
}
