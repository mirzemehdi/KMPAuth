# Phone number sign-in

State lives in `kmpauth-core` and is served by whichever backend is
registered — [Firebase](firebase.md) or [Supabase](supabase.md):

- **Firebase**: Android (with automatic SMS verification when Play services
  can) and iOS. Enable the "Phone" sign-in method in the Firebase console.
  Desktop, web and wasm report a failed `Result` (the REST/web flows need a
  reCAPTCHA verifier KMPAuth does not provide).
- **Supabase**: **every target** — plain OTP over SMS. Enable the Phone
  provider and an SMS sender (Twilio, Vonage, ...) in the Supabase
  dashboard.

Two-step flow: `launch()` sends the SMS, `submitCode` completes sign-in:

```kotlin
var phoneNumber by remember { mutableStateOf("") }
var smsCode by remember { mutableStateOf("") }
val phoneSignIn = rememberPhoneAuthState(
    phoneNumber = phoneNumber, // E.164 format, e.g. +15551234567
    onResult = { result: Result<KMPAuthUser> -> },
)

if (!phoneSignIn.isCodeSent) {
    Button(onClick = { phoneSignIn.launch() }) { Text("Send code") }
} else {
    OutlinedTextField(value = smsCode, onValueChange = { smsCode = it })
    Button(onClick = { phoneSignIn.submitCode(smsCode) }) { Text("Verify") }
}
```

`phoneSignIn.cancel()` abandons the flow (e.g. the user dismissed the code
input). On Android with Firebase, Play services may auto-verify the SMS —
then the flow completes directly and `isCodeSent` never turns true.

Outside a composable, the same flow is
`KMPAuth.signInWithPhone(phoneNumber, verificationUi)` — implement
`PhoneVerificationUi.awaitVerificationCode()` to supply the code.

Custom backends serve this flow by implementing
`AuthProviderBackend.signInWithPhone`.
