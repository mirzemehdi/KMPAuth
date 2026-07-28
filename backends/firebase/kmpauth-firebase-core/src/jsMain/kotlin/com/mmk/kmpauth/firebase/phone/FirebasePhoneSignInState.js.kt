package com.mmk.kmpauth.firebase.phone

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import dev.gitlive.firebase.auth.FirebaseUser

@Composable
public actual fun rememberFirebasePhoneSignInState(
    phoneNumber: String,
    linkAccount: Boolean,
    onCodeSent: () -> Unit,
    onResult: (Result<FirebaseUser?>) -> Unit,
): PhoneSignInState {
    val currentOnResult by rememberUpdatedState(onResult)
    return remember {
        UnsupportedPhoneSignInState(
            onResult = { currentOnResult(it) },
            reason = "Phone sign-in on the web requires a reCAPTCHA verifier, " +
                "which KMPAuth does not provide yet.",
        )
    }
}
