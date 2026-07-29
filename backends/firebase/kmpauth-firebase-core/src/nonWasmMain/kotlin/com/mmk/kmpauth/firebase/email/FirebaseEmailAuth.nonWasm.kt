package com.mmk.kmpauth.firebase.email

import com.mmk.kmpauth.core.KMPAuthInternalApi
import com.mmk.kmpauth.core.auth.KMPAuthUser
import com.mmk.kmpauth.core.runCatchingCancellable
import com.mmk.kmpauth.firebase.backend.FirebaseKMPAuthUser
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.EmailAuthProvider
import dev.gitlive.firebase.auth.auth

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
