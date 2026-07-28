package com.mmk.kmpauth.firebase.phone

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.mmk.kmpauth.core.KMPAuthInternalApi
import com.mmk.kmpauth.core.SignInState
import com.mmk.kmpauth.core.logger.currentLogger
import com.mmk.kmpauth.core.runCatchingCancellable
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.PhoneAuthProvider
import dev.gitlive.firebase.auth.PhoneVerificationProvider
import dev.gitlive.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Handle returned by [rememberFirebasePhoneSignInState]. Phone sign-in is a
 * two-step flow, so this extends [SignInState] with the second step:
 *
 * 1. [launch] sends the SMS verification code to the phone number.
 * 2. [isCodeSent] turns true (and `onCodeSent` fires) — show your code
 *    input UI and pass what the user typed to [submitCode].
 *
 * On Android, Google Play services can auto-verify the SMS without user
 * input; in that case the flow completes directly and [isCodeSent] never
 * turns true.
 */
@Stable
public interface PhoneSignInState : SignInState {

    /**
     * True after the verification code was sent, while the flow waits for
     * [submitCode].
     */
    public val isCodeSent: Boolean

    /**
     * Completes the flow with the SMS verification [code] the user
     * received. Ignored when no launched flow is waiting for a code.
     */
    public fun submitCode(code: String)

    /**
     * Abandons an in-progress flow (e.g. the user dismissed the code input
     * UI). `onResult` is not called for a cancelled flow.
     */
    public fun cancel()
}

/**
 * Phone number sign-in with Firebase as a Compose state holder.
 *
 * [phoneNumber] is read at launch time: pass the current value of your
 * phone-number field, and [SignInState.launch] uses whatever is current
 * when the user taps.
 *
 * ```
 * var phoneNumber by remember { mutableStateOf("") }
 * var smsCode by remember { mutableStateOf("") }
 * val phoneSignIn = rememberFirebasePhoneSignInState(
 *     phoneNumber = phoneNumber,
 *     onResult = onFirebaseResult,
 * )
 *
 * if (!phoneSignIn.isCodeSent) {
 *     Button(onClick = { phoneSignIn.launch() }) { Text("Send code") }
 * } else {
 *     TextField(value = smsCode, onValueChange = { smsCode = it })
 *     Button(onClick = { phoneSignIn.submitCode(smsCode) }) { Text("Verify") }
 * }
 * ```
 *
 * Enable the "Phone" sign-in method in the Firebase console first.
 *
 * Platform support: Android (with automatic SMS verification when Play
 * services can) and iOS. On Desktop (JVM) the underlying Firebase SDK does
 * not implement phone auth, and on JS/web Firebase requires a reCAPTCHA
 * verifier KMPAuth does not provide yet — both report a failed [Result].
 *
 * @param phoneNumber Phone number in E.164 format (e.g. `+15551234567`),
 * read at launch time.
 * @param linkAccount true links the phone credential to the currently
 * signed-in Firebase user instead of creating a new session — e.g. to
 * upgrade an anonymous user to a permanent account.
 * @param onCodeSent invoked when the SMS was sent and the flow starts
 * waiting for [PhoneSignInState.submitCode] — show your code input UI.
 * @param onResult receives the signed-in [FirebaseUser] or the failure
 * (invalid phone number, quota exceeded, invalid code, ...).
 */
@Composable
public expect fun rememberFirebasePhoneSignInState(
    phoneNumber: String,
    linkAccount: Boolean = false,
    onCodeSent: () -> Unit = {},
    onResult: (Result<FirebaseUser?>) -> Unit,
): PhoneSignInState

/**
 * Shared [PhoneSignInState] implementation. Platform actuals supply
 * [createVerificationProvider], which builds the platform's
 * [PhoneVerificationProvider] around the shared code-await logic: the
 * provider's `getVerificationCode` must call the given `getCode` lambda,
 * which flips [isCodeSent], fires `onCodeSent` and suspends until
 * [submitCode] delivers the code.
 */
internal class PhoneSignInStateImpl(
    private val scope: CoroutineScope,
    private val phoneNumber: () -> String,
    private val linkAccount: () -> Boolean,
    private val onCodeSent: () -> Unit,
    private val onResult: (Result<FirebaseUser?>) -> Unit,
    private val createVerificationProvider: (getCode: suspend () -> String) -> PhoneVerificationProvider,
) : PhoneSignInState {

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
    private suspend fun signIn(): Result<FirebaseUser?> = runCatchingCancellable {
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
        result.user
    }

    override fun submitCode(code: String) {
        codeDeferred?.complete(code)
    }

    override fun cancel() {
        job?.cancel()
    }
}

/**
 * [PhoneSignInState] for platforms where phone sign-in cannot work;
 * launching reports [reason] as a failed [Result] instead of crashing.
 */
internal class UnsupportedPhoneSignInState(
    private val onResult: (Result<FirebaseUser?>) -> Unit,
    private val reason: String,
) : PhoneSignInState {
    override val isInProgress: Boolean = false
    override val isCodeSent: Boolean = false
    override fun launch(): Unit = onResult(Result.failure(UnsupportedOperationException(reason)))
    override fun submitCode(code: String): Unit = Unit
    override fun cancel(): Unit = Unit
}
