# GitHub, Microsoft and other OAuth providers

The states live in `kmpauth-core` and are served by whichever backend is
registered — the same composable runs against Firebase or Supabase:

```kotlin
val githubSignIn = rememberGithubAuthState(onResult = { result: Result<KMPAuthUser> -> })
Button(onClick = { githubSignIn.launch() }) { Text("Sign in with GitHub") }

val microsoftSignIn = rememberMicrosoftAuthState(onResult = { result: Result<KMPAuthUser> -> })

// any other provider:
val yahooSignIn = rememberOAuthState(provider = "yahoo.com", onResult = { result -> })
```

Options: `requestScopes` (GitHub default `["user:email"]`, Microsoft
`["mail.read"]`), `customParameters` (e.g.
`mapOf("tenant" to "your-tenant-id")` to restrict Microsoft to one Azure AD
tenant), `linkAccount`.

Non-composable equivalent:
`KMPAuth.signIn(AuthCredential.OAuthWebFlow("github.com"))`.

## How each backend runs the flow

**Firebase** (enable the provider in the Firebase console; register the app
in the Azure portal for Microsoft):

- **Android / iOS**: the Firebase SDK's native web-flow UI.
- **Desktop (JVM)**: the system browser opens a local page running
  Firebase's official JS SDK against your project's hosted auth handler —
  every console-configured provider works, including Apple. See
  [the Firebase guide](firebase.md#desktop-jvm).
- **Web (JS/wasm)**: not implemented yet — reports a failed `Result`.

**Supabase** (enable the provider in the dashboard; `provider` also accepts
GoTrue names like `github`, `azure`, `gitlab`, `discord`):

- **Desktop (JVM)**: works out of the box — supabase-kt catches the
  redirect on its own localhost callback server.
- **Android / iOS**: supabase-kt's standard deep-link setup.
- **Web (JS/wasm)**: full-page redirect; the session is restored after
  reload. Details: [the Supabase guide](supabase.md#browser-oauth-per-platform).
