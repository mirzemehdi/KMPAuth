# Firebase backend

Module: `kmpauth-firebase`. Serves every `rememberXxxAuthState` flow and
`KMPAuth.*` operation, and hosts the
[Apple/GitHub/Microsoft/OAuth](oauth-providers.md) and [phone](phone.md)
states.

## Registration is automatic

With `kmpauth-firebase` in your dependencies the backend registers itself —
`ServiceLoader` on JVM/Android (the R8 keep rule ships in the consumer
rules, nothing to configure for minified builds) and eager load-time
registration on iOS/JS/wasm. No setup call.

## Android / iOS

Zero configuration: the Firebase SDK reads its bundled
`google-services.json` / `GoogleService-Info.plist`.

## Desktop (JVM)

Provide the web app config once at start:

```kotlin
KMPAuth.initialize {
    firebase(apiKey = "...", projectId = "...", applicationId = "...")
}
```

Desktop support is complete:

- **Email/password, email link, anonymous, reauthentication and
  Google/Facebook/Apple token exchange** run against the **Firebase Auth
  REST API** (GitLive's firebase-java-sdk has no auth implementation). The
  session is held in memory — no disk persistence yet.
- **Browser flows** ([OAuth/GitHub/Microsoft/Apple](oauth-providers.md))
  open the system browser on a local page that runs Firebase's official JS
  SDK against your project's hosted auth handler
  (`https://<authDomain>/__/auth/handler`) — every provider configured in
  the Firebase console works, including Apple. Nothing but that single flow
  is served on the loopback.
- **Phone** stays unavailable (needs reCAPTCHA).

## Web

- **JS**: email/anonymous/Google run through the Firebase JS SDK (provide
  the web config via `firebase(...)` as on Desktop). The browser-flow
  states (OAuth/GitHub/Microsoft/Apple) and phone are not implemented yet
  and report a failed `Result`.
- **wasm**: the Firebase SDK has no wasm target — all Firebase flows report
  a failed `Result`. Use the [Supabase backend](supabase.md) for wasm.

## Escape hatch

`KMPAuthUser.raw` is the GitLive `dev.gitlive.firebase.auth.FirebaseUser`
on platforms where GitLive serves the session (Android/iOS/JS), letting you
call Firebase APIs KMPAuth doesn't wrap.
