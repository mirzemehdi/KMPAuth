package com.mmk.kmpauth.firebase.anonymous

import com.mmk.kmpauth.core.KMPAuthInternalApi
import com.mmk.kmpauth.core.auth.KMPAuthUser
import com.mmk.kmpauth.core.runCatchingCancellable
import com.mmk.kmpauth.firebase.backend.FirebaseKMPAuthUser
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth

@OptIn(KMPAuthInternalApi::class)
internal actual suspend fun firebaseAnonymousSignIn(): Result<KMPAuthUser?> =
    runCatchingCancellable {
        Firebase.auth.signInAnonymously().user?.let(::FirebaseKMPAuthUser)
    }
