# Anonymous (guest) sign-in

State lives in `kmpauth-core` and is served by whichever backend is
registered — [Firebase](firebase.md) or [Supabase](supabase.md). Enable the
"Anonymous" sign-in method in your backend's console first.

Lets users try the app before creating an account:

```kotlin
val anonymousSignIn = rememberAnonymousAuthState(onResult = { result: Result<KMPAuthUser> -> })
Button(onClick = { anonymousSignIn.launch() }) { Text("Continue as guest") }
```

## Upgrading a guest to a permanent account

Sign in with any auth state using `linkAccount = true` (e.g.
`rememberEmailAuthState(..., linkAccount = true)`) — the credential is
linked to the anonymous user, keeping its uid and data.
