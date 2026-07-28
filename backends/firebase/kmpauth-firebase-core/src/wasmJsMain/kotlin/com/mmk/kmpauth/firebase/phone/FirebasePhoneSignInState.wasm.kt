package com.mmk.kmpauth.firebase.phone

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.mmk.kmpauth.core.auth.KMPAuthUser
import com.mmk.kmpauth.firebase.backend.WASM_UNSUPPORTED_REASON

@Composable
public actual fun rememberFirebasePhoneSignInState(
    phoneNumber: String,
    linkAccount: Boolean,
    onCodeSent: () -> Unit,
    onResult: (Result<KMPAuthUser?>) -> Unit,
): PhoneSignInState {
    val currentOnResult by rememberUpdatedState(onResult)
    return remember {
        UnsupportedPhoneSignInState(
            onResult = { currentOnResult(it) },
            reason = WASM_UNSUPPORTED_REASON,
        )
    }
}
