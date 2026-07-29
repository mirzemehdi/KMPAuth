# UI helper buttons

Module: `kmpauth-uihelper`. Pre-styled buttons following each brand's
guidelines — mix freely with your own designs:

```kotlin
GoogleSignInButton(modifier = Modifier.fillMaxWidth()) { googleSignIn.launch() }
GoogleSignInButtonIconOnly(onClick = { googleSignIn.launch() })

AppleSignInButton(modifier = Modifier.fillMaxWidth()) { appleSignIn.launch() }
AppleSignInButtonIconOnly(onClick = { appleSignIn.launch() })

FacebookSignInButton(modifier = Modifier.fillMaxWidth()) { facebookSignIn.launch() }
FacebookSignInButtonIconOnly(onClick = { facebookSignIn.launch() })
```

Buttons are plain composables — any clickable can drive a sign-in state, so
these are optional sugar.
