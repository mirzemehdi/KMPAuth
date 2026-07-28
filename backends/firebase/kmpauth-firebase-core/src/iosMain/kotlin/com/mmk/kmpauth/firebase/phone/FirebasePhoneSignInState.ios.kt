package com.mmk.kmpauth.firebase.phone

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import cocoapods.FirebaseAuth.FIRAuthUIDelegateProtocol
import com.mmk.kmpauth.core.auth.KMPAuthBackend
import com.mmk.kmpauth.firebase.backend.FirebaseAuthBackend
import com.mmk.kmpauth.core.auth.KMPAuthUser
import dev.gitlive.firebase.auth.PhoneVerificationProvider
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
@Composable
public actual fun rememberFirebasePhoneSignInState(
    phoneNumber: String,
    linkAccount: Boolean,
    onCodeSent: () -> Unit,
    onResult: (Result<KMPAuthUser?>) -> Unit,
): PhoneSignInState {
    val scope = rememberCoroutineScope()
    val currentPhoneNumber by rememberUpdatedState(phoneNumber)
    val currentLinkAccount by rememberUpdatedState(linkAccount)
    val currentOnCodeSent by rememberUpdatedState(onCodeSent)
    val currentOnResult by rememberUpdatedState(onResult)

    return remember {
        // Lazy default registration: no-op when the app already registered
        // a backend at startup (first registration wins).
        KMPAuthBackend.register(FirebaseAuthBackend)
        PhoneSignInStateImpl(
            scope = scope,
            phoneNumber = { currentPhoneNumber },
            linkAccount = { currentLinkAccount },
            onCodeSent = { currentOnCodeSent() },
            onResult = { currentOnResult(it) },
            createVerificationProvider = { getCode ->
                object : PhoneVerificationProvider {
                    // null lets the Firebase SDK present its reCAPTCHA
                    // fallback UI over the top view controller.
                    override val delegate: FIRAuthUIDelegateProtocol? = null
                    override suspend fun getVerificationCode(): String = getCode()
                }
            },
        )
    }
}
