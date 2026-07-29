# Getting started

## 1. Add the dependencies

KMPAuth is on Maven Central:

```kotlin
repositories {
    mavenCentral()
}
```

Pick what you need in your shared module's `commonMain`
(latest version: [![Maven Central](https://img.shields.io/maven-central/v/io.github.mirzemehdi/kmpauth-google?color=blue)](https://search.maven.org/search?q=g:io.github.mirzemehdi+kmpauth)):

```kotlin
sourceSets {
    commonMain.dependencies {
        // Identity providers (credential only - no backend required):
        implementation("io.github.mirzemehdi:kmpauth-google:<version>")
        implementation("io.github.mirzemehdi:kmpauth-facebook:<version>")
        implementation("io.github.mirzemehdi:kmpauth-apple:<version>")

        // Session backend - pick one (or both, see the backend guides):
        implementation("io.github.mirzemehdi:kmpauth-firebase:<version>")
        implementation("io.github.mirzemehdi:kmpauth-supabase:<version>")

        // Pre-styled "Sign in with ..." buttons:
        implementation("io.github.mirzemehdi:kmpauth-uihelper:<version>")
    }
}
```

| Artifact | What it gives you |
|---|---|
| `kmpauth-google` | Google sign-in — [guide](google.md) |
| `kmpauth-facebook` | Facebook login — [guide](facebook.md) |
| `kmpauth-apple` | Native Sign in with Apple (Apple platforms, no backend) — [guide](apple.md) |
| `kmpauth-firebase` | Firebase auth backend + Apple/GitHub/Microsoft/OAuth/phone states — [guide](firebase.md) |
| `kmpauth-supabase` | Supabase auth backend — [guide](supabase.md) |
| `kmpauth-uihelper` | Google/Apple/Facebook buttons per each brand's guidelines — [guide](ui-helper.md) |
| `kmpauth-firebase-google`, `kmpauth-firebase-facebook` | **Deprecated** 2.x container shims only — not needed in new code, removed in 4.0 |

## 2. iOS: add the native SDKs via Swift Package Manager

CocoaPods is no longer supported as of 3.0 — see [MIGRATION.md](../MIGRATION.md).
In Xcode, **File → Add Package Dependencies…**:

| You use | Add package | Products |
|---|---|---|
| `kmpauth-google` | `https://github.com/google/GoogleSignIn-iOS` | `GoogleSignIn` |
| `kmpauth-firebase` | `https://github.com/firebase/firebase-ios-sdk` | `FirebaseAuth`, `FirebaseCore` |
| `kmpauth-facebook` | `https://github.com/facebook/facebook-ios-sdk` | `FacebookCore`, `FacebookLogin` |

> [!NOTE]
> If you get `MissingResourceException` on iOS, see the solution in
> [this issue's comments](https://github.com/mirzemehdi/KMPAuth/issues/2).

## 3. Requirements

| | Minimum |
|---|---|
| iOS deployment target | 16.0 |
| Xcode | 16.4 |
| JVM runtime (desktop apps) | 17 |
| Android compileSdk | 37 |

## 4. Initialize once at application start

Provider and backend modules plug their setup into the same block:

```kotlin
KMPAuth.initialize {
    logger { println("KMPAuthLog: $it") }          // optional
    google(serverId = WebClientId)                  // kmpauth-google
    // Firebase on Desktop/Web also needs the web config (no-op on Android/iOS):
    firebase(apiKey = "...", projectId = "...", applicationId = "...")
    // ...or Supabase instead of Firebase:
    // supabase(url = projectUrl, apiKey = publishableKey)
}
```

With `kmpauth-firebase` in your dependencies **the Firebase backend
registers itself automatically** — on Android/iOS, where the Firebase SDK
reads its bundled config files, no `firebase(...)` call is needed at all.
Details: [Firebase backend](firebase.md).

## 5. Sign in

Drop a sign-in state next to any button:

```kotlin
val googleSignIn = rememberGoogleAuthState(onResult = { result ->
    result.onSuccess { user -> println("Signed in: ${user.uid}") }
        .onFailure { error -> println("Failed: $error") }
})
GoogleSignInButton { googleSignIn.launch() }
```

Session and account operations live on the `KMPAuth` object:

```kotlin
val user: KMPAuthUser? = KMPAuth.currentUser()
KMPAuth.signOut()
```

## Next

- [Core concepts](core-concepts.md) — the two state layers, `KMPAuthUser`, the `KMPAuth` object, backends
- Provider guides: [Google](google.md) · [Apple](apple.md) · [Facebook](facebook.md) · [GitHub/Microsoft/OAuth](oauth-providers.md) · [Email](email.md) · [Phone](phone.md) · [Anonymous](anonymous.md)
- The [sample app](../sampleApp/shared/src/commonMain/kotlin/com/mmk/kmpauth/sample/App.kt) exercises every feature
