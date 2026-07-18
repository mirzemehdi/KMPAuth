# Changelog

All notable changes to KMPAuth are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Changed
- **Facebook on Android: classic login no longer requires `onActivityResult`.**
  With `FacebookLoginTracking.Enabled`, KMPAuth now launches Facebook Login
  through the SDK's `logInWithReadPermissions(ActivityResultRegistryOwner,
  CallbackManager, permissions)` overload (the pattern used by Facebook's own
  Compose sample), so apps no longer need to override `onActivityResult` or
  call `KMPAuth.handleFacebookActivityResult` for that mode.
  `FacebookLoginTracking.Limited` (the default) still needs it: Limited Login
  requires a nonce, which the Facebook SDK only accepts via
  `LoginConfiguration`, and that API has no `ActivityResultRegistryOwner`
  overload — the SDK exposes one only as a `private` function. Calling
  `handleFacebookActivityResult` when it is not needed remains harmless (#199).

## [3.0.0-alpha01] — 2026-07-08

See [MIGRATION.md](MIGRATION.md) for the step-by-step 2.x → 3.0 upgrade guide.

### Added
- **`SignInState` API.** New `rememberXxxSignInState(...)` composables
  (`rememberGoogleSignInState`, `rememberFacebookSignInState`,
  `rememberFirebaseGoogleSignInState`, `rememberFirebaseAppleSignInState`,
  `rememberFirebaseGithubSignInState`, `rememberFirebaseOAuthSignInState`,
  `rememberFirebaseFacebookSignInState`) return a `SignInState` handle with
  `launch()` and an observable `isInProgress`. Wire `launch()` to any
  clickable; parameters such as `linkAccount` are read at launch time, so
  toggling them via recomposition works; double-taps cannot start two flows.
- **Granular Firebase artifacts.** `kmpauth-firebase` is split into
  `kmpauth-firebase-core` (FirebaseAuthBackend + Apple/GitHub/OAuth
  containers) and `kmpauth-firebase-google` (Google + Firebase container).
  The `kmpauth-firebase` artifact remains published as an aggregator of
  both (its module lives under `deprecated/` in the repo), so existing
  dependency declarations keep working; depend on the granular artifacts
  to avoid pulling the Google Sign-In stack into apps that don't use it.
- **wasmJs on every module.** `kmpauth-firebase` and
  `kmpauth-firebase-facebook` now compile for wasmJs too, so a wasm-targeting
  app can keep them in `commonMain` dependencies. Their Firebase API surface
  exists on android/ios/jvm/js only (GitLive has no wasm target); on wasm the
  modules resolve as empty artifacts.
- **Pluggable auth backends.** New `com.mmk.kmpauth.core.auth` API in
  `kmpauth-core`: `AuthProviderBackend` (sign-in/sign-out/current-user over
  a backend-agnostic `AuthCredential` + `KMPAuthUser` model) and the
  `KMPAuthBackend` registry. Firebase remains the default implementation
  (registered automatically by `kmpauth-firebase`); a Supabase backend can
  plug in via `KMPAuthBackend.register(...)` without any changes to UI code.
- Characterization test suite locking the public 2.x behavior (DI lifecycle,
  `GoogleAuthProvider` entry points, sign-in overload defaults, public model shapes).
- **`FacebookLoginTracking` option.** `rememberFacebookSignInState`,
  `rememberFirebaseFacebookSignInState` and the Facebook containers accept a
  `loginTracking` parameter (default `Limited`). `Limited` uses Facebook's
  privacy-friendly Limited Login (OIDC JWT + nonce; no App Tracking
  Transparency prompt on iOS); `Enabled` uses classic login and returns a
  Graph-API access token. The mode now determines the token type consistently
  on **both** Android and iOS, and the Firebase exchange picks the matching
  credential (`OAuthProvider` for Limited, `FacebookAuthProvider` for Enabled)
  (#170).

### Changed
- **Facebook iOS/Android token consistency.** `FacebookUser.accessToken` now
  holds the same token type on both platforms for a given `loginTracking`
  mode. Because the new default is `FacebookLoginTracking.Limited`, **Android
  now returns an OIDC JWT + nonce by default instead of a Graph-API access
  token** (iOS was already Limited). Apps that send `FacebookUser.accessToken`
  to a backend expecting a Graph-API access token must pass
  `loginTracking = FacebookLoginTracking.Enabled` (#170).
- Sample restructured into `sampleApp/shared` plus per-platform entry
  modules (`androidApp`, `desktopApp`, `webApp`, `iosApp`) per the AGP 9
  layout; the iOS framework is now named `shared`. A web sample (js) is new.
- Build toolchain: Android Gradle Plugin 9.2 (`com.android.kotlin.multiplatform.library`),
  Gradle 9.4.1. **JVM target raised from 1.8 to 17** for the android and jvm
  artifacts — consumers need a Java 17+ runtime for desktop/JVM apps.
- Android artifacts are now published as a **single variant**; the separate
  `debug` variant is no longer published. Debug builds resolve the release
  variant automatically.
- Android host (unit) test task is now `testAndroid` (was
  `testDebugUnitTest`/`testReleaseUnitTest`).
- Kotlin 2.2.21 → **2.4.0**; Compose Multiplatform plugin 1.9.3 → 1.11.1; Ktor 3.5.1,
  kotlinx-serialization 1.11.0, kotlinx-coroutines 1.11.0,
  Facebook Android SDK 18.3.0, androidx.credentials 1.6.0, googleid 1.2.0,
  Dokka 2.2.0, vanniktech maven-publish 0.37.0, Firebase Android BoM 34.15.0.
- Android `compileSdk` raised to 37 (required by androidx.core 1.19);
  `targetSdk`/`minSdk` unchanged (36/24).
- **iOS dependencies now integrate via Swift Package Manager instead of
  CocoaPods.** The `kmpauth_*.podspec` files are no longer published; add
  GoogleSignIn-iOS / firebase-ios-sdk / facebook-ios-sdk as SPM packages in
  your Xcode project instead (see MIGRATION.md). iOS minimum deployment
  target raised from 12.0 to **16.0**; building the library requires
  Xcode 16.4+.

### Deprecated
- All `*UiContainer` composables (`GoogleButtonUiContainer`,
  `GoogleButtonUiContainerFirebase`, `AppleButtonUiContainer`,
  `GithubButtonUiContainer`, `OAuthContainer`, `FacebookButtonUiContainer`,
  `FacebookButtonUiContainerFirebase`) in favor of the `SignInState` API.
  They keep working as thin wrappers over the new implementation and are
  slated for removal in 4.0.
- The parameter-less legacy overloads of `GoogleButtonUiContainerFirebase`,
  `AppleButtonUiContainer`, `GithubButtonUiContainer` and `OAuthContainer`
  (deprecated since 2.x in favor of the `linkAccount`/
  `filterByAuthorizedAccounts` overloads) remain deprecated with warnings
  and are slated for removal in 4.0.

### Fixed
- **Desktop (JVM) Google Sign-In redirect URI.** The loopback server used a
  random port that never matched the `redirect_uri` sent to Google, so the
  callback failed (`redirect_uri_mismatch`, blank callback page, or
  `id_token=null` on a second attempt). The redirect is now fixed and
  configurable via `GoogleAuthCredentials(serverId, redirectUri = "http://localhost:8080/callback")` —
  pass the exact **Authorized redirect URI** registered for your OAuth client
  in the Google Cloud console (any `http` loopback host/port/path;
  defaults to `http://localhost:8080/callback`). The callback server binds the
  URI's port and serves its path; when the port is already in use the failure
  is logged clearly instead of silently falling back to a random port. The
  browser now opens only after the server is listening (#172, #177, #181).

### Removed
- **Koin.** KMPAuth no longer uses or ships Koin. The `@KMPAuthInternalApi`
  DI types `KMPKoinComponent` and `LibDependencyInitializer` are deleted and
  the `io.insert-koin:koin-core` dependency is gone from every module.
  Public entry points (`GoogleAuthProvider.create`, all `*UiContainer`
  composables) are unchanged — internal wiring is now plain constructor
  injection. Only code that referenced those two internal-API types directly
  is affected (see MIGRATION.md).
- **`iosX64` target.** Dropped from all modules (Compose Multiplatform 1.11+
  no longer ships iosX64 artifacts; Intel-Mac simulators can use Rosetta
  with the arm64 simulator target). It was briefly re-added in 2.5.0-alpha01.
- Stale `api/android/*.api` compatibility dumps (no longer generated or
  validated under AGP 9's KMP library plugin; the klib and JVM dumps remain
  the source of truth).

## [2.5.0-alpha01] — 2026-06

### Added
- Dedicated Facebook auth modules: `kmpauth-facebook` (SDK-only) and
  `kmpauth-firebase-facebook` (Facebook + Firebase) (#163).
- `iosX64` target restored (#164).

### Fixed
- Google JVM sign-in uses a dynamically found port for the OAuth redirect URI (#165).

## [2.4.0] — 2025

### Added
- `setAutoSelectEnabled(isAutoSelectEnabled)` on Google sign-in (#126).
- Non-legacy Google sign-in with custom scopes on Android (#132).

### Fixed
- Icon and text alignment in `FacebookSignInButton` (#142).
- Compatibility with Firebase Android BoM 34.0.0 (#141).

_Older releases: see [GitHub releases](https://github.com/mirzemehdi/KMPAuth/releases)._
