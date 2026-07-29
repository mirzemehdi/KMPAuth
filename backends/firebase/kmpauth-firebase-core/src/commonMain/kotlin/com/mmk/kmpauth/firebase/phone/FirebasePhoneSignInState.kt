package com.mmk.kmpauth.firebase.phone

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import com.mmk.kmpauth.core.SignInState
import com.mmk.kmpauth.core.auth.KMPAuthUser

/**
 * Handle returned by [rememberPhoneAuthState]. Phone sign-in is a
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
 * Phone number sign-in with Firebase as a Compose state holder.
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
 * not implement phone auth, on JS/web Firebase requires a reCAPTCHA
 * verifier KMPAuth does not provide yet, and on wasm the SDK has no
 * target — all three report a failed [Result].
 *
 * @param phoneNumber Phone number in E.164 format (e.g. `+15551234567`),
 * read at launch time.
 * @param linkAccount true links the phone credential to the currently
 * signed-in Firebase user instead of creating a new session — e.g. to
 * upgrade an anonymous user to a permanent account.
 * @param onCodeSent invoked when the SMS was sent and the flow starts
 * waiting for [PhoneAuthState.submitCode] — show your code input UI.
 * @param onResult receives the signed-in [KMPAuthUser] or the failure
 * (invalid phone number, quota exceeded, invalid code, ...). The native
 * Firebase user stays reachable through [KMPAuthUser.raw].
 */
@Composable
public expect fun rememberPhoneAuthState(
    phoneNumber: String,
    linkAccount: Boolean = false,
    onCodeSent: () -> Unit = {},
    onResult: (Result<KMPAuthUser>) -> Unit,
): PhoneAuthState

/**
 * [PhoneAuthState] for platforms where phone sign-in cannot work;
 * launching reports [reason] as a failed [Result] instead of crashing.
 */
internal class UnsupportedPhoneAuthState(
    private val onResult: (Result<KMPAuthUser>) -> Unit,
    private val reason: String,
) : PhoneAuthState {
    override val isInProgress: Boolean = false
    override val isCodeSent: Boolean = false
    override fun launch(): Unit = onResult(Result.failure(UnsupportedOperationException(reason)))
    override fun submitCode(code: String): Unit = Unit
    override fun cancel(): Unit = Unit
}
