package com.mmk.kmpauth.firebase.phone

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.mmk.kmpauth.core.KMPAuthInternalApi
import com.mmk.kmpauth.core.auth.KMPAuthBackend
import com.mmk.kmpauth.core.getActivity
import com.mmk.kmpauth.firebase.backend.FirebaseAuthBackend
import com.mmk.kmpauth.core.auth.KMPAuthUser
import dev.gitlive.firebase.auth.PhoneVerificationProvider
import java.util.concurrent.TimeUnit

@OptIn(KMPAuthInternalApi::class)
@Composable
public actual fun rememberPhoneAuthState(
    phoneNumber: String,
    linkAccount: Boolean,
    onCodeSent: () -> Unit,
    onResult: (Result<KMPAuthUser>) -> Unit,
): PhoneAuthState {
    val activity = LocalContext.current.getActivity()
    val scope = rememberCoroutineScope()
    val currentPhoneNumber by rememberUpdatedState(phoneNumber)
    val currentLinkAccount by rememberUpdatedState(linkAccount)
    val currentOnCodeSent by rememberUpdatedState(onCodeSent)
    val currentOnResult by rememberUpdatedState(onResult)

    return remember {
        // Lazy default registration: no-op when the app already registered
        // a backend at startup (first registration wins).
        KMPAuthBackend.registerDefault(FirebaseAuthBackend)
        PhoneAuthStateImpl(
            scope = scope,
            phoneNumber = { currentPhoneNumber },
            linkAccount = { currentLinkAccount },
            onCodeSent = { currentOnCodeSent() },
            onResult = { currentOnResult(it) },
            createVerificationProvider = { getCode ->
                object : PhoneVerificationProvider {
                    // The Firebase SDK needs the Activity to run its
                    // reCAPTCHA/Play Integrity fallback verification UI.
                    override val activity: Activity = requireNotNull(activity) {
                        "Phone sign-in requires an Activity context"
                    }
                    override val timeout: Long = 60
                    override val unit: TimeUnit = TimeUnit.SECONDS
                    override fun codeSent(triggerResend: (Unit) -> Unit) = Unit
                    override suspend fun getVerificationCode(): String = getCode()
                }
            },
        )
    }
}
