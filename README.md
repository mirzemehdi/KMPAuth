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

## Pick your setup

### 1. No backend — you verify the token yourself

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

### 2. Firebase

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

→ [Firebase guide](docs/firebase.md)

### 3. Supabase

Add `kmpauth-supabase` — no Firebase anywhere, works on **every target
including wasm**. Same states, same `KMPAuth` operations; only the
registration differs:

```kotlin
KMPAuth.initialize {
    google(serverId = WebClientId)
    supabase(url = projectUrl, apiKey = publishableKey)
}

val onResult: (Result<KMPAuthUser>) -> Unit = { result -> /* ... */ }

val googleSignIn = rememberGoogleAuthState(onResult = onResult)     // id-token grant
val emailSignIn = rememberEmailAuthState(email, password, onResult = onResult)
val guestSignIn = rememberAnonymousAuthState(onResult = onResult)

KMPAuth.sendPasswordResetEmail(email)
KMPAuth.signOut()
```

→ [Supabase guide](docs/supabase.md) (Ktor engine per platform, what maps to Supabase)

Need Firebase **and** Supabase side by side? One `CompositionLocalProvider`
wrapper — [Custom & multiple backends](docs/custom-backends.md).

## Documentation

Start here — read only what you need:

| Guide | What's in it |
|---|---|
| **[Getting started](docs/getting-started.md)** | Dependencies, iOS SPM setup, requirements, `KMPAuth.initialize`, first sign-in |
| **[Core concepts](docs/core-concepts.md)** | The two state layers, `KMPAuthUser`, the `KMPAuth` object, account linking, reauthentication |

**Sign-in providers:**

| Guide | Platforms |
|---|---|
| [Google](docs/google.md) | Android · iOS · Desktop · JS · wasm |
| [Apple](docs/apple.md) | Android · iOS (native) · Desktop — or iOS-only without any backend |
| [Facebook](docs/facebook.md) | Android · iOS |
| [GitHub / Microsoft / any OAuth](docs/oauth-providers.md) | Android · iOS · Desktop |
| [Email — password, reset, magic link](docs/email.md) | everywhere the backend runs |
| [Phone number](docs/phone.md) | Android · iOS |
| [Anonymous (guest)](docs/anonymous.md) | everywhere the backend runs |

**Auth backends:**

| Guide | What's in it |
|---|---|
| [Firebase](docs/firebase.md) | Auto-registration, Android/iOS zero-config, Desktop (REST + browser flows), web notes |
| [Supabase](docs/supabase.md) | Setup, Ktor engines, what maps to Supabase (works on **wasm**) |
| [Custom & multiple backends](docs/custom-backends.md) | `AuthProviderBackend`, `LocalKMPAuthBackend` scoping |

Also: [UI helper buttons](docs/ui-helper.md) ·
[Full API reference](https://mirzemehdi.github.io/KMPAuth) ·
[Sample app covering every feature](sampleApp/shared/src/commonMain/kotlin/com/mmk/kmpauth/sample/App.kt)

## What's supported where

| Feature | Android | iOS | Desktop (JVM) | Web (JS) | Web (wasm) |
|---|:---:|:---:|:---:|:---:|:---:|
| Google sign-in | ✅ | ✅ | ✅ | ✅ | ✅ |
| Apple sign-in (via Firebase) | ✅ | ✅ native | ✅ | — | — |
| Apple sign-in (native, no backend) | — | ✅ | — | — | — |
| Facebook sign-in | ✅ | ✅ | — | — | — |
| GitHub / Microsoft / any OAuth (via Firebase) | ✅ | ✅ | ✅ | — | — |
| Email password / reset / magic link | ✅ | ✅ | ✅ | ✅ | ✅ Supabase |
| Phone number sign-in | ✅ | ✅ | — | — | — |
| Anonymous sign-in | ✅ | ✅ | ✅ | ✅ | ✅ Supabase |
| Firebase backend | ✅ | ✅ | ✅ REST | ✅ | — |
| Supabase backend | ✅ | ✅ | ✅ | ✅ | ✅ |

Everything compiles and is callable from `commonMain` on **all** targets — a
feature unavailable on the current platform reports a failed `Result` with
the reason instead of not compiling or silently doing nothing.

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
