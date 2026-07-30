package com.mmk.kmpauth.core.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import com.mmk.kmpauth.core.KMPAuthInternalApi
import com.mmk.kmpauth.core.SignInState
import com.mmk.kmpauth.core.logger.currentLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * UI hooks an [AuthProviderBackend] uses while running a phone sign-in —
 * see [AuthProviderBackend.signInWithPhone]. [rememberPhoneAuthState]
 * supplies an implementation wired to its [PhoneAuthState]; only callers
 * driving [com.mmk.kmpauth.core.KMPAuth.signInWithPhone] outside a
 * composable implement this themselves.
 */
public interface PhoneVerificationUi {

    /**
     * Called when the backend has sent the SMS and needs the verification
     * code the user received; suspends until it is available. The state
     * implementation flips `isCodeSent` here and resumes from
     * [PhoneAuthState.submitCode].
     */
    public suspend fun awaitVerificationCode(): String

    /**
     * Platform UI handle some backends need to run fallback verification
     * UI — the Android `Activity` for Firebase's reCAPTCHA/Play Integrity
     * fallback. Null on other platforms and for backends that don't need
     * one (e.g. Supabase's plain OTP flow).
     */
    public val platformUiContext: Any? get() = null
}

/**
 * Handle returned by [rememberPhoneAuthState]. Phone sign-in is a
 * two-step flow, so this extends [SignInState] with the second step:
 *
 * 1. [launch] sends the SMS verification code to the phone number.
 * 2. [isCodeSent] turns true (and `onCodeSent` fires) — show your code
 *    input UI and pass what the user typed to [submitCode].
 *
 * On Android with the Firebase backend, Google Play services can
 * auto-verify the SMS without user input; in that case the flow completes
 * directly and [isCodeSent] never turns true.
 */
@Stable
public interface PhoneAuthState : SignInState {

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
 * Phone number sign-in as a Compose state holder, served by the registered
 * auth backend (Firebase or Supabase today; any [AuthProviderBackend]).
 *
 * [phoneNumber] is read at launch time: pass the current value of your
 * phone-number field, and [SignInState.launch] uses whatever is current
 * when the user taps.
 *
 * ```
 * var phoneNumber by remember { mutableStateOf("") }
 * var smsCode by remember { mutableStateOf("") }
 * val phoneSignIn = rememberPhoneAuthState(
 *     phoneNumber = phoneNumber,
 *     onResult = onAuthResult,
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
 * Backend support:
 * - **Firebase**: Android (with automatic SMS verification when Play
 *   services can) and iOS. Enable the "Phone" sign-in method in the
 *   Firebase console. On Desktop the Firebase SDK does not implement phone
 *   auth, on JS/web it requires a reCAPTCHA verifier KMPAuth does not
 *   provide yet, and wasm has no Firebase SDK — those report a failed
 *   [Result].
 * - **Supabase**: every target — plain OTP over SMS. Enable the Phone
 *   provider and an SMS sender (Twilio, Vonage, ...) in the Supabase
 *   dashboard.
 *
 * @param phoneNumber Phone number in E.164 format (e.g. `+15551234567`),
 * read at launch time.
 * @param linkAccount true links the phone credential to the currently
 * signed-in user instead of creating a new session — e.g. to upgrade an
 * anonymous user to a permanent account (backend support varies).
 * @param onCodeSent invoked when the SMS was sent and the flow starts
 * waiting for [PhoneAuthState.submitCode] — show your code input UI.
 * @param onResult receives the signed-in [KMPAuthUser] or the failure
 * (invalid phone number, quota exceeded, invalid code, ...). The backend's
 * native user stays reachable through [KMPAuthUser.raw].
 */
@Composable
public fun rememberPhoneAuthState(
    phoneNumber: String,
    linkAccount: Boolean = false,
    onCodeSent: () -> Unit = {},
    onResult: (Result<KMPAuthUser>) -> Unit,
): PhoneAuthState {
    val scope = rememberCoroutineScope()
    val currentPhoneNumber by rememberUpdatedState(phoneNumber)
    val currentLinkAccount by rememberUpdatedState(linkAccount)
    val currentOnCodeSent by rememberUpdatedState(onCodeSent)
    val currentOnResult by rememberUpdatedState(onResult)
    val currentBackend by rememberUpdatedState(LocalKMPAuthBackend.current)
    val currentUiContext by rememberUpdatedState(phoneAuthPlatformUiContext())

    return remember {
        PhoneAuthStateImpl(
            scope = scope,
            phoneNumber = { currentPhoneNumber },
            linkAccount = { currentLinkAccount },
            onCodeSent = { currentOnCodeSent() },
            onResult = { currentOnResult(it) },
            backend = { currentBackend },
            platformUiContext = { currentUiContext },
        )
    }
}

/**
 * Platform UI handle passed to the backend as
 * [PhoneVerificationUi.platformUiContext]: the current `Activity` on
 * Android, null elsewhere.
 */
@Composable
internal expect fun phoneAuthPlatformUiContext(): Any?

/**
 * Shared [PhoneAuthState] implementation: runs
 * [AuthProviderBackend.signInWithPhone] with a [PhoneVerificationUi] whose
 * `awaitVerificationCode` flips [isCodeSent], fires `onCodeSent` and
 * suspends until [submitCode] delivers the code.
 */
internal class PhoneAuthStateImpl(
    private val scope: CoroutineScope,
    private val phoneNumber: () -> String,
    private val linkAccount: () -> Boolean,
    private val onCodeSent: () -> Unit,
    private val onResult: (Result<KMPAuthUser>) -> Unit,
    private val backend: () -> AuthProviderBackend,
    private val platformUiContext: () -> Any?,
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
                val deferred = CompletableDeferred<String>()
                codeDeferred = deferred
                val verificationUi = object : PhoneVerificationUi {
                    override suspend fun awaitVerificationCode(): String {
                        isCodeSent = true
                        onCodeSent()
                        return deferred.await()
                    }

                    override val platformUiContext: Any? =
                        this@PhoneAuthStateImpl.platformUiContext()
                }
                onResult(
                    backend().signInWithPhone(
                        phoneNumber = phoneNumber(),
                        verificationUi = verificationUi,
                        linkWithCurrentUser = linkAccount(),
                    )
                )
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

    override fun submitCode(code: String) {
        codeDeferred?.complete(code)
    }

    override fun cancel() {
        job?.cancel()
    }
}
