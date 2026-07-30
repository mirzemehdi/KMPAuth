# Apple Sign-In

Two flavors, both in `kmpauth-apple`:

| | Returns | Platforms | Backend |
|---|---|---|---|
| `rememberAppleAuthState` | `Result<KMPAuthUser>` session | every target the backend serves | Firebase or Supabase — whichever is registered |
| `rememberAppleSignInState` | `Result<AppleUser>` raw credential | iOS only | none — you verify the token |

## Session (`rememberAppleAuthState`)

Enable the Apple provider in the Firebase console / Supabase dashboard and
add the **"Sign In with Apple" capability** in Xcode:

```kotlin
val appleSignIn = rememberAppleAuthState(onResult = { result: Result<KMPAuthUser> -> })
AppleSignInButton { appleSignIn.launch() }
```

How it runs:

- **iOS**: native AuthenticationServices flow; the identity token is
  exchanged through the backend's `id_token` grant — including the user's
  full name, which Apple returns **only on the first authorization** (the
  Firebase backend persists it as the display name).
- **Android / Desktop**: the backend's browser OAuth web flow for
  `apple.com` (Firebase's hosted handler on Desktop works out of the box).
- **Web**: Firebase — not implemented yet; Supabase — full-page redirect
  (see [the Supabase guide](supabase.md#browser-oauth-per-platform)).

## Native token, no backend (`rememberAppleSignInState`)

When your own server verifies Apple's identity token. The native flow
returns a signed JWT that any backend can validate against Apple's public
keys — no client secret involved:

```kotlin
val appleSignIn = rememberAppleSignInState(onResult = { result ->
    val appleUser = result.getOrNull()
    val idToken = appleUser?.idToken   // send to your backend
    val rawNonce = appleUser?.nonce    // if your backend verifies the nonce claim
})
AppleSignInButton { appleSignIn.launch() }
```

> **Apple platforms only.** On Android, JVM, JS and wasm
> `rememberAppleSignInState` reports a failed `Result` — Apple's web flow
> returns an authorization code that must be exchanged with a **client
> secret server-side**. Use `rememberAppleAuthState` there.
>
> `email` and `fullName` are returned by Apple **only on the user's first
> authorization** — persist them server-side; later sign-ins return null.
