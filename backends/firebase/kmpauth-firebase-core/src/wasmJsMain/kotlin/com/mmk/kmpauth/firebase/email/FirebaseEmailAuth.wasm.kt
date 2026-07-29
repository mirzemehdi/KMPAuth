package com.mmk.kmpauth.firebase.email

import com.mmk.kmpauth.core.auth.KMPAuthUser
import com.mmk.kmpauth.firebase.backend.WASM_UNSUPPORTED_REASON

internal actual suspend fun firebaseEmailSignIn(
    email: String,
    password: String,
    mode: EmailAuthMode,
    linkAccount: Boolean,
): Result<KMPAuthUser?> =
    Result.failure(UnsupportedOperationException(WASM_UNSUPPORTED_REASON))
