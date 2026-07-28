package com.mmk.kmpauth.firebase.anonymous

import com.mmk.kmpauth.core.auth.KMPAuthUser
import com.mmk.kmpauth.firebase.backend.WASM_UNSUPPORTED_REASON

internal actual suspend fun firebaseAnonymousSignIn(): Result<KMPAuthUser?> =
    Result.failure(UnsupportedOperationException(WASM_UNSUPPORTED_REASON))
