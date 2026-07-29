# Phone number sign-in

Module: `kmpauth-firebase`. Android (with automatic SMS verification when
Play services can) and iOS. Enable the "Phone" sign-in method in the
Firebase console.

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
input).

On Desktop and web, launching reports a failed `Result` — the Firebase Java
SDK does not implement phone auth, and the web flow would need a reCAPTCHA
verifier KMPAuth does not provide yet.
