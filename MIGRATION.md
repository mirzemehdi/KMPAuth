# Migrating KMPAuth 2.x → 3.0

Step-by-step guide for upgrading from KMPAuth 2.x to 3.0.

## TL;DR checklist

- [ ] Bump `io.github.mirzemehdi:kmpauth-*` to `3.0.0`.
- [ ] iOS: replace CocoaPods pods with Swift Package Manager packages;
      raise your app's iOS deployment target to 16.0 and use Xcode 16.4+.
- [ ] Desktop/JVM: run on Java 17+ (artifacts now target JVM 17 bytecode).
- [ ] If you referenced the `@KMPAuthInternalApi` DI types directly
      (`KMPKoinComponent`, `LibDependencyInitializer`): remove those usages —
      they are deleted in 3.0. Public entry points (`GoogleAuthProvider.create`,
      the `*UiContainer` composables) are unchanged and need no code changes.
- [ ] Address `@Deprecated` warnings — each carries a `ReplaceWith` migration hint.

## What did NOT change

- All public composable containers keep their 2.x signatures:
  `GoogleButtonUiContainer`, `GoogleButtonUiContainerFirebase`,
  `AppleButtonUiContainer`, `GithubButtonUiContainer`, `OAuthContainer`,
  `FacebookButtonUiContainer`, `FacebookButtonUiContainerFirebase`, and the
  `kmpauth-uihelper` buttons.
- `GoogleAuthProvider.create(credentials)` initialization flow.
- Public models: `GoogleUser`, `GoogleAuthCredentials`, `FacebookUser`,
  request-scope types.
- Maven coordinates (`io.github.mirzemehdi:kmpauth-*`).

## 1. iOS: CocoaPods → Swift Package Manager

KMPAuth 3.0 no longer ships `kmpauth_*.podspec` files and no longer uses the
`kotlin("native.cocoapods")` plugin. The library declares its Apple
frameworks through the Kotlin `swiftPMDependencies {}` DSL (Kotlin 2.4+).

**In your iOS app (Xcode):**
1. Remove the KMPAuth-related pods from your `Podfile` (`kmpauth_google`,
   `kmpauth_firebase`, `kmpauth_facebook`, `kmpauth_firebase_facebook`) and
   run `pod install`; if KMPAuth was your only pod, deintegrate CocoaPods
   entirely (`pod deintegrate`).
2. Add the native SDKs your setup uses via **File → Add Package
   Dependencies…**:
   - Google Sign-In: `https://github.com/google/GoogleSignIn-iOS` (product `GoogleSignIn`), 9.1.0+
   - Firebase: `https://github.com/firebase/firebase-ios-sdk` (products `FirebaseAuth`, `FirebaseCore`), 11.8.0+
   - Facebook: `https://github.com/facebook/facebook-ios-sdk` (products `FacebookCore`, `FacebookLogin`), 18.0.0+
3. Set your app's iOS deployment target to **16.0+** and use **Xcode 16.4+**.
4. Keep integrating your shared framework the same way as before
   (`embedAndSignAppleFrameworkForXcode` build phase or XCFramework).

**In your shared KMP module:** if you consume KMPAuth from a KMP project
that itself builds the iOS framework, no Kotlin code changes are needed —
the `cocoapods.FirebaseAuth.*` bindings inside `kmpauth-firebase` come from
GitLive's bundled cinterop and keep working.

## 2. Koin removal

KMPAuth no longer uses (or ships) Koin — internal wiring is plain
constructor injection.

**Most users need no changes.** `GoogleAuthProvider.create(credentials)` and
all `*UiContainer` composables behave exactly as in 2.x (first `create()`
wins, `get()` before `create()` throws the same `IllegalArgumentException`).

You are only affected if you referenced the `@KMPAuthInternalApi`-annotated
DI types directly (both deleted in 3.0):

- `com.mmk.kmpauth.core.di.KMPKoinComponent` — was an internal bridge to
  KMPAuth's private Koin container; there is no replacement. If you were
  resolving KMPAuth types through it, use the public entry points instead.
- `com.mmk.kmpauth.core.di.LibDependencyInitializer` — initialization now
  happens inside `GoogleAuthProvider.create(...)`; simply delete the call.

If your app uses Koin itself, nothing changes — KMPAuth never shared your
application's Koin container (it ran a private `koinApplication`), so no
modules need removing from your setup.

## 3. Toolchain requirements

| Requirement | 2.x | 3.0 |
|---|---|---|
| iOS deployment target | 11.0–12.0 | 16.0 |
| Xcode | 15+ | 16.4+ |
| JVM bytecode | 1.8 | 17 |
| Android debug variant artifact | published | no longer published (single-variant; debug builds resolve the release variant automatically) |

**JVM 17:** the `kmpauth-*` android and jvm artifacts now contain JVM 17
bytecode. Android builds are unaffected (AGP 9 requires JDK 17 anyway);
desktop/JVM apps must run on Java 17+.

**Single-variant Android artifacts:** if your build explicitly pinned the
`-debug` artifact (rare), drop the suffix — Gradle resolves the single
published variant for all build types automatically.

**Firebase BoM on Android:** `kmpauth-firebase` now exposes the Firebase
Android BoM constraint (`api(platform(firebase-bom))`), so Firebase artifact
versions are pinned automatically. If you were pinning Firebase versions
yourself to work around resolution errors, you can remove those pins.

## 4. Pluggable auth backends

3.0 introduces `com.mmk.kmpauth.core.auth.AuthProviderBackend` with a
backend-agnostic credential/user model (`AuthCredential`, `KMPAuthUser`).

- **Existing Firebase users: no action.** `kmpauth-firebase` registers
  `FirebaseAuthBackend` automatically the first time a container is used;
  every composable keeps returning `Result<FirebaseUser?>` exactly as in 2.x.
- **Custom/Supabase backends:** implement `AuthProviderBackend` and call
  `KMPAuthBackend.register(yourBackend)` at application start (before any
  KMPAuth UI renders). The first registration wins, so an explicit
  registration always beats the lazy Firebase default; pass
  `replace = true` to swap an already-registered backend.
- Web-flow providers (Apple on Android, GitHub, generic OAuth) are executed
  by their dedicated container composables — a backend `signIn` call cannot
  drive a browser flow.
