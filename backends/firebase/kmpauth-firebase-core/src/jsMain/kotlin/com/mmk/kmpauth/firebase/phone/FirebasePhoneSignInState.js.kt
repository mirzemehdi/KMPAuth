package com.mmk.kmpauth.firebase.phone

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.mmk.kmpauth.core.auth.KMPAuthUser

@Composable
public actual fun rememberPhoneAuthState(
    phoneNumber: String,
    linkAccount: Boolean,
    onCodeSent: () -> Unit,
    onResult: (Result<KMPAuthUser>) -> Unit,
): PhoneAuthState {
    val currentOnResult by rememberUpdatedState(onResult)
    return remember {
        UnsupportedPhoneAuthState(
            onResult = { currentOnResult(it) },
            reason = "Phone sign-in on the web requires a reCAPTCHA verifier, " +
                "which KMPAuth does not provide yet.",
        )
    }
}
