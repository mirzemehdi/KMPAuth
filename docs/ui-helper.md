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

Each text button takes `mode` (brand color variants), `text`, `shape`, and an
`iconAlignment`:

- `SignInButtonIconAlignment.Center` (default) — icon and title centered as
  one group, like the providers' official buttons. The icon's position depends
  on the title width, so icons of stacked buttons won't line up exactly.
- `SignInButtonIconAlignment.Start` — icon pinned at the leading edge, title
  centered on the button axis. A stacked column of full-width buttons keeps
  every provider icon and every title aligned.

```kotlin
AppleSignInButton(
    modifier = Modifier.fillMaxWidth().height(44.dp),
    iconAlignment = SignInButtonIconAlignment.Start,
) { appleAuth.launch() }
```
