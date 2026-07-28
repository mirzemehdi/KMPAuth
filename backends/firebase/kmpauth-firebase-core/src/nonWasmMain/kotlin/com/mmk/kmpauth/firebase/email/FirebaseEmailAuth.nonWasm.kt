package com.mmk.kmpauth.firebase.email

import com.mmk.kmpauth.core.KMPAuthInternalApi
import com.mmk.kmpauth.core.auth.KMPAuthUser
import com.mmk.kmpauth.core.runCatchingCancellable
import com.mmk.kmpauth.firebase.backend.FirebaseKMPAuthUser
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.ActionCodeSettings
import dev.gitlive.firebase.auth.AndroidPackageName
import dev.gitlive.firebase.auth.EmailAuthProvider
import dev.gitlive.firebase.auth.auth

/** Maps the SDK-agnostic settings onto GitLive's ActionCodeSettings. */
private fun EmailActionCodeSettings.toFirebase(): ActionCodeSettings = ActionCodeSettings(
    url = url,
    canHandleCodeInApp = canHandleCodeInApp,
    iOSBundleId = iOSBundleId,
    androidPackageName = androidPackageName?.let {
        AndroidPackageName(
            packageName = it,
            installIfNotAvailable = androidInstallIfNotAvailable,
            minimumVersion = androidMinimumVersion,
        )
    },
    linkDomain = linkDomain,
)

@OptIn(KMPAuthInternalApi::class)
internal actual suspend fun firebaseEmailSignIn(
    email: String,
    password: String,
    mode: EmailAuthMode,
    linkAccount: Boolean,
): Result<KMPAuthUser?> = runCatchingCancellable {
    val auth = Firebase.auth
    val currentUser = auth.currentUser
    val result = if (linkAccount && currentUser != null) {
        currentUser.linkWithCredential(EmailAuthProvider.credential(email, password))
    } else when (mode) {
        EmailAuthMode.SignIn -> auth.signInWithEmailAndPassword(email, password)
        EmailAuthMode.SignUp -> auth.createUserWithEmailAndPassword(email, password)
    }
    result.user?.let(::FirebaseKMPAuthUser)
}

@OptIn(KMPAuthInternalApi::class)
internal actual suspend fun firebaseSendPasswordResetEmail(
    email: String,
    actionCodeSettings: EmailActionCodeSettings?,
): Result<Unit> = runCatchingCancellable {
    Firebase.auth.sendPasswordResetEmail(email, actionCodeSettings?.toFirebase())
}

@OptIn(KMPAuthInternalApi::class)
internal actual suspend fun firebaseSendSignInLinkToEmail(
    email: String,
    actionCodeSettings: EmailActionCodeSettings,
): Result<Unit> = runCatchingCancellable {
    Firebase.auth.sendSignInLinkToEmail(email, actionCodeSettings.toFirebase())
}

internal actual fun firebaseIsSignInWithEmailLink(link: String): Boolean =
    Firebase.auth.isSignInWithEmailLink(link)

@OptIn(KMPAuthInternalApi::class)
internal actual suspend fun firebaseSignInWithEmailLink(
    email: String,
    link: String,
    linkAccount: Boolean,
): Result<KMPAuthUser?> = runCatchingCancellable {
    val auth = Firebase.auth
    val currentUser = auth.currentUser
    val result = if (linkAccount && currentUser != null) {
        currentUser.linkWithCredential(EmailAuthProvider.credentialWithLink(email, link))
    } else {
        auth.signInWithEmailLink(email, link)
    }
    result.user?.let(::FirebaseKMPAuthUser)
}

@OptIn(KMPAuthInternalApi::class)
internal actual suspend fun firebaseReauthenticate(
    email: String,
    password: String,
): Result<Unit> = runCatchingCancellable {
    val currentUser = Firebase.auth.currentUser
        ?: throw IllegalStateException("No signed-in user to reauthenticate")
    currentUser.reauthenticate(EmailAuthProvider.credential(email, password))
}
