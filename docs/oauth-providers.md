# GitHub, Microsoft and other OAuth providers

Module: `kmpauth-firebase` — Firebase drives the OAuth web flow, no
provider SDK involved. Works on Android, iOS and Desktop (JVM); on Web
(JS/wasm) these states are not implemented yet and report a failed
`Result`.

## GitHub

Enable the GitHub provider in the Firebase console:

```kotlin
val githubSignIn = rememberGithubAuthState(onResult = { result: Result<KMPAuthUser> -> })
Button(onClick = { githubSignIn.launch() }) { Text("Sign in with GitHub") }
```

Options: `requestScopes` (default `["user:email"]`), `customParameters`,
`linkAccount`.

## Microsoft

Enable the Microsoft provider in the Firebase console and register the app
in the Azure portal:

```kotlin
val microsoftSignIn = rememberMicrosoftAuthState(onResult = { result: Result<KMPAuthUser> -> })
Button(onClick = { microsoftSignIn.launch() }) { Text("Sign in with Microsoft") }
```

To restrict sign-in to one Azure AD tenant, pass
`customParameters = mapOf("tenant" to "your-tenant-id")`.

## Any other OAuth provider

Any OAuth provider configured in the Firebase console works through the
generic state (GitHub and Microsoft above are thin wrappers over it):

```kotlin
val yahooSignIn = rememberOAuthState(
    provider = "yahoo.com",
    onResult = { result: Result<KMPAuthUser> -> },
)
```

## How it runs per platform

- **Android / iOS**: Firebase SDK's native web-flow UI.
- **Desktop (JVM)**: the state opens the system browser on a local page that
  runs Firebase's official JS SDK against your project's hosted auth
  handler — every console-configured provider works, including Apple. See
  the [Firebase backend guide](firebase.md#desktop-jvm).
