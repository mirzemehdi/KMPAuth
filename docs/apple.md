# Apple Sign-In

Two flavors:

| | Module | Platforms | Backend |
|---|---|---|---|
| `rememberAppleAuthState` | `kmpauth-firebase` | Android, iOS (native), Desktop | Firebase session |
| `rememberAppleSignInState` | `kmpauth-apple` | iOS only | none — you verify the token |

**With Supabase** instead: on iOS exchange the native credential —
`KMPAuth.signIn(AuthCredential.IdToken(AuthProviderIds.APPLE, appleUser.idToken, rawNonce = appleUser.nonce))`
— and on other platforms use the browser flow,
`KMPAuth.signIn(AuthCredential.OAuthWebFlow("apple.com"))` (see the
[Supabase guide](supabase.md#browser-oauth-per-platform)).

## With Firebase (all platforms)

Enable Apple in the Firebase console and add the **"Sign In with Apple"
capability** in Xcode. On iOS the flow is native; on Android/Desktop
Firebase drives Apple's web flow:

```kotlin
val appleSignIn = rememberAppleAuthState(onResult = { result: Result<KMPAuthUser> -> })
AppleSignInButton { appleSignIn.launch() }
```

## Native, without a backend (Apple platforms only)

Use `kmpauth-apple` when your own server verifies Apple's identity token.
The native flow returns a signed JWT that any backend can validate against
Apple's public keys — no client secret involved:

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
> returns an authorization code that must be exchanged with a **client secret
> server-side**. Use `rememberAppleAuthState` there; Firebase performs the
> exchange for you.
>
> `email` and `fullName` are returned by Apple **only on the user's first
> authorization** — persist them server-side; later sign-ins return null.
