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
            reason = "Phone sign-in is not supported on Desktop: the Firebase " +
                "Java SDK does not implement phone authentication " +
                "(https://github.com/mirzemehdi/KMPAuth/issues/204).",
        )
    }
}
