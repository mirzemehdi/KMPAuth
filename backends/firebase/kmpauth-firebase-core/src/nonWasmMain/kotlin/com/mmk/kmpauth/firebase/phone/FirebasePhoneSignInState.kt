package com.mmk.kmpauth.firebase.phone

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.mmk.kmpauth.core.KMPAuthInternalApi
import com.mmk.kmpauth.core.auth.KMPAuthUser
import com.mmk.kmpauth.core.logger.currentLogger
import com.mmk.kmpauth.core.runCatchingCancellable
import com.mmk.kmpauth.firebase.backend.FirebaseKMPAuthUser
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.PhoneAuthProvider
import dev.gitlive.firebase.auth.PhoneVerificationProvider
import dev.gitlive.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Shared [PhoneAuthState] implementation. Platform actuals supply
 * [createVerificationProvider], which builds the platform's
 * [PhoneVerificationProvider] around the shared code-await logic: the
 * provider's `getVerificationCode` must call the given `getCode` lambda,
 * which flips [isCodeSent], fires `onCodeSent` and suspends until
 * [submitCode] delivers the code.
 */
internal class PhoneAuthStateImpl(
    private val scope: CoroutineScope,
    private val phoneNumber: () -> String,
    private val linkAccount: () -> Boolean,
    private val onCodeSent: () -> Unit,
    private val onResult: (Result<KMPAuthUser>) -> Unit,
    private val createVerificationProvider: (getCode: suspend () -> String) -> PhoneVerificationProvider,
) : PhoneAuthState {

    override var isInProgress: Boolean by mutableStateOf(false)
        private set

    override var isCodeSent: Boolean by mutableStateOf(false)
        private set

    private var codeDeferred: CompletableDeferred<String>? = null
    private var job: Job? = null

    @OptIn(KMPAuthInternalApi::class)
    override fun launch() {
        if (isInProgress) return
        job = scope.launch {
            isInProgress = true
            try {
                onResult(signIn())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                // Flows report failures through their onResult callback; an
                // exception escaping here would otherwise crash the app via
                // the composition's coroutine scope.
                currentLogger.log("Phone sign-in flow failed with uncaught exception: $e")
            } finally {
                isInProgress = false
                isCodeSent = false
                codeDeferred = null
            }
        }
    }

    @OptIn(KMPAuthInternalApi::class)
    private suspend fun signIn(): Result<KMPAuthUser> = runCatchingCancellable {
        val deferred = CompletableDeferred<String>()
        codeDeferred = deferred
        val verificationProvider = createVerificationProvider {
            isCodeSent = true
            onCodeSent()
            deferred.await()
        }
        val credential =
            PhoneAuthProvider().verifyPhoneNumber(phoneNumber(), verificationProvider)
        val auth = Firebase.auth
        val currentUser = auth.currentUser
        val result = if (linkAccount() && currentUser != null) {
            currentUser.linkWithCredential(credential)
        } else {
            auth.signInWithCredential(credential)
        }
        result.user?.let(::FirebaseKMPAuthUser)
            ?: throw IllegalStateException("Firebase Null user")
    }

    override fun submitCode(code: String) {
        codeDeferred?.complete(code)
    }

    override fun cancel() {
        job?.cancel()
    }
}
