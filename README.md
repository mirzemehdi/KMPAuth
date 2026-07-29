# KMPAuth — Kotlin Multiplatform Authentication Library

[![Build](https://github.com/mirzemehdi/KMPAuth/actions/workflows/build_and_publish.yml/badge.svg)](https://github.com/mirzemehdi/KMPAuth/actions/workflows/build_and_publish.yml)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.4.0-blue.svg?style=flat&logo=kotlin)](https://kotlinlang.org)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.mirzemehdi/kmpauth-google?color=blue)](https://search.maven.org/search?q=g:io.github.mirzemehdi+kmpauth)

![badge-android](http://img.shields.io/badge/platform-android-6EDB8D.svg?style=flat)
![badge-ios](http://img.shields.io/badge/platform-ios-AAAAFF.svg?style=flat)
![badge-desktop](http://img.shields.io/badge/platform-desktop-FF8E8E.svg?style=flat)
![badge-web](http://img.shields.io/badge/platform-web-FFCC66.svg?style=flat)

Simple and easy-to-use authentication for Compose Multiplatform apps on
**Android, iOS, Desktop (JVM) and Web (JS + wasm)**. Sign in with **Google,
Apple, Facebook, GitHub, Microsoft, email/password, magic links, phone
number or anonymously** — backed by **Firebase** or **Supabase** (or your
own backend), with every API callable from `commonMain` on every target.

<p style="text-align: center;">
  <img src="https://github.com/mirzemehdi/KMPAuth/assets/32781662/f5a3cd28-6ef2-46bf-9b07-a045ce217b34)" width="200" alt="SampleApp"/>
</p>

## What's supported where

Every sign-in method is served by a **backend** (or by your own server, when
the method just hands you a token). So two questions: which backend runs on
your platforms, and which methods that backend serves.

| Backend | Android | iOS | Desktop (JVM) | Web (JS) | Web (wasm) |
|---|:---:|:---:|:---:|:---:|:---:|
| Firebase | ✅ | ✅ | ✅ (REST) | ✅ | ✅ (REST²) |
| Supabase | ✅ | ✅ | ✅ | ✅ | ✅ |

("your server" below = no backend needed — the state hands you the
provider's token to verify yourself.)

| Sign-in method | Works with | Android | iOS | Desktop (JVM) | Web (JS) | Web (wasm) |
|---|---|:---:|:---:|:---:|:---:|:---:|
| Google | your server · Firebase · Supabase | ✅ | ✅ | ✅ | ✅ | ✅ |
| Apple | Firebase · Supabase | ✅ | ✅ native | ✅ | ✅⁴ | ✅⁴ |
| Apple (native token, no backend) | your server | — | ✅ | — | — | — |
| Facebook (native SDK login) | your server · Firebase · Supabase¹ | ✅ | ✅ | — | — | — |
| GitHub / Microsoft / Facebook-web / any OAuth | Firebase · Supabase | ✅ | ✅ | ✅ | ✅⁴ | ✅⁴ |
| Email (password / reset / magic link) | Firebase · Supabase | ✅ | ✅ | ✅ | ✅ | ✅ |
| Phone number | Firebase · Supabase | ✅ | ✅ | ✅³ | ✅³ | ✅³ |
| Anonymous | Firebase · Supabase | ✅ | ✅ | ✅ | ✅ | ✅ |

¹ Supabase accepts Facebook Limited Login (OIDC) tokens only. Meta's SDK
exists only on Android/iOS — on other platforms use the browser-OAuth row
(`rememberOAuthState("facebook.com")` with Firebase, or
`OAuthWebFlow("facebook.com")` with Supabase).
² Firebase on wasm runs on the REST engine (no Firebase SDK there): email,
anonymous, id-token exchange, email link, password reset, reauthentication —
not browser web flows or phone.
³ Beyond Android/iOS only with Supabase (SMS OTP); Firebase phone auth
needs the mobile SDKs.
⁴ Only with Supabase (`KMPAuth.signIn(AuthCredential.OAuthWebFlow("github.com"))`):
Desktop works out of the box, Android/iOS need Supabase's deep-link setup,
and on web the flow is a full-page redirect — the session is restored after
reload.

Everything compiles and is callable from `commonMain` on **all** targets — a
feature unavailable on the current platform reports a failed `Result` with
the reason instead of not compiling or silently doing nothing.

## Pick your setup

### 1. No backend — you verify the token yourself ([Google](docs/google.md) · [Facebook](docs/facebook.md) · [Apple](docs/apple.md))

Only the provider modules (`kmpauth-google`, `kmpauth-facebook`,
`kmpauth-apple`). The `rememberXxxSignInState` states hand you the
provider's credential and stop there — send it to your own server:

```kotlin
KMPAuth.initialize {
    google(serverId = WebClientId)
}

val googleSignIn = rememberGoogleSignInState(onResult = { result ->
    result.onSuccess { googleUser ->
        api.login(googleUser.idToken) // verify server-side
    }.onFailure { error -> /* cancelled, misconfigured, ... */ }
})
GoogleSignInButton { googleSignIn.launch() }

// same shape for Facebook and native Apple:
val facebookSignIn = rememberFacebookSignInState(onResult = { result: Result<FacebookUser> -> })
val appleSignIn = rememberAppleSignInState(onResult = { result: Result<AppleUser> -> })
```

### 2. Firebase ([guide](docs/firebase.md))

Add `kmpauth-firebase` — the backend registers itself, and on Android/iOS
there is zero configuration (the SDK reads `google-services.json` /
`GoogleService-Info.plist`). The `rememberXxxAuthState` states exchange the
credential for a Firebase session; account operations live on `KMPAuth`:

```kotlin
KMPAuth.initialize {
    google(serverId = WebClientId)
    // Desktop/Web only - Android/iOS use the bundled config files:
    firebase(apiKey = "...", projectId = "...", applicationId = "...")
}

val onResult: (Result<KMPAuthUser>) -> Unit = { result -> /* ... */ }

val googleSignIn = rememberGoogleAuthState(onResult = onResult)
GoogleSignInButton { googleSignIn.launch() }

val appleSignIn = rememberAppleAuthState(onResult = onResult)       // native on iOS, web flow elsewhere
val facebookSignIn = rememberFacebookAuthState(onResult = onResult) // kmpauth-facebook, Android/iOS
val githubSignIn = rememberGithubAuthState(onResult = onResult)     // Firebase OAuth web flow
val microsoftSignIn = rememberMicrosoftAuthState(onResult = onResult)
val emailSignIn = rememberEmailAuthState(email, password, onResult = onResult)
val phoneSignIn = rememberPhoneAuthState(phoneNumber, onResult = onResult)
val guestSignIn = rememberAnonymousAuthState(onResult = onResult)

KMPAuth.currentUser()
KMPAuth.signOut()
KMPAuth.sendPasswordResetEmail(email)
KMPAuth.reauthenticate(AuthCredential.EmailPassword(email, password))
```

### 3. Supabase ([guide](docs/supabase.md))

Add `kmpauth-supabase` (plus a Ktor client engine per platform — see the
guide) — no Firebase anywhere, works on **every target including wasm**.
Same states, same `KMPAuth` operations; only the registration differs:

```kotlin
KMPAuth.initialize {
    google(serverId = WebClientId)
    supabase(url = projectUrl, apiKey = publishableKey)
}

val onResult: (Result<KMPAuthUser>) -> Unit = { result -> /* ... */ }

val googleSignIn = rememberGoogleAuthState(onResult = onResult)     // id-token grant
val facebookSignIn = rememberFacebookAuthState(onResult = onResult) // kmpauth-facebook, Limited Login (OIDC), Android/iOS
val emailSignIn = rememberEmailAuthState(email, password, onResult = onResult)
val phoneSignIn = rememberPhoneAuthState(phoneNumber, onResult = onResult) // SMS OTP, every target
val guestSignIn = rememberAnonymousAuthState(onResult = onResult)

// Browser OAuth - GitHub/Microsoft/GitLab/any GoTrue provider
// (Desktop works out of the box; see the guide for mobile deep links):
val githubResult = KMPAuth.signIn(AuthCredential.OAuthWebFlow("github.com"))

KMPAuth.sendPasswordResetEmail(email)
KMPAuth.signOut()
```

Need Firebase **and** Supabase side by side? One `CompositionLocalProvider`
wrapper — [Custom & multiple backends](docs/custom-backends.md).

## Documentation

Start here — read only what you need:

| Guide | What's in it |
|---|---|
| **[Getting started](docs/getting-started.md)** | Dependencies, iOS SPM setup, requirements, `KMPAuth.initialize`, first sign-in |
| **[Core concepts](docs/core-concepts.md)** | The two state layers, `KMPAuthUser`, the `KMPAuth` object, account linking, reauthentication |

**Identity providers** (bring their own SDK/flow, work with any backend or
none):

| Guide | Platforms |
|---|---|
| [Google](docs/google.md) | Android · iOS · Desktop · JS · wasm |
| [Apple](docs/apple.md) | Android · iOS (native) · Desktop — or iOS-only without any backend |
| [Facebook](docs/facebook.md) | Android · iOS |

**Backends & backend-served sign-in** (these flows exist only through
Firebase/Supabase):

| Guide | What's in it |
|---|---|
| [Firebase](docs/firebase.md) | Auto-registration, Android/iOS zero-config, Desktop (REST + browser flows), web notes |
| [Supabase](docs/supabase.md) | Setup, Ktor engines, what maps to Supabase (works on **wasm**) |
| [GitHub / Microsoft / any OAuth](docs/oauth-providers.md) | Firebase (Android · iOS · Desktop) or Supabase (every target, see guide) |
| [Email — password, reset, magic link](docs/email.md) | Served by Firebase or Supabase |
| [Phone number](docs/phone.md) | Firebase (Android · iOS) or Supabase (every target) |
| [Anonymous (guest)](docs/anonymous.md) | Served by Firebase or Supabase |
| [Custom & multiple backends](docs/custom-backends.md) | `AuthProviderBackend`, `LocalKMPAuthBackend` scoping |

Also: [UI helper buttons](docs/ui-helper.md) ·
[Full API reference](https://mirzemehdi.github.io/KMPAuth) ·
[Sample app covering every feature](sampleApp/shared/src/commonMain/kotlin/com/mmk/kmpauth/sample/App.kt)

## Installation (short version)

```kotlin
commonMain.dependencies {
    implementation("io.github.mirzemehdi:kmpauth-google:<version>")   // Google sign-in
    implementation("io.github.mirzemehdi:kmpauth-firebase:<version>") // Firebase backend
    implementation("io.github.mirzemehdi:kmpauth-uihelper:<version>") // branded buttons
    // also available: kmpauth-facebook, kmpauth-apple, kmpauth-supabase
}
```

iOS apps add the native SDKs via Swift Package Manager — see
[Getting started](docs/getting-started.md).

## Migrating from 2.x

Follow the step-by-step [MIGRATION.md](MIGRATION.md) — most 2.x code keeps
compiling (the `*UiContainer` composables and `GoogleAuthProvider.create`
still work, deprecated). All notable changes live in
[CHANGELOG.md](CHANGELOG.md).

---

KMPAuth powers [FindTravelNow](https://github.com/mirzemehdi/FindTravelNow-KMM/),
a production KMP app, so development is actively supported. Related blog
post: [Integrating Google Sign-In into Kotlin Multiplatform](https://proandroiddev.com/integrating-google-sign-in-into-kotlin-multiplatform-8381c189a891).
