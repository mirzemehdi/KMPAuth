# Changelog

All notable changes to KMPAuth are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased] — 3.0.0

See [MIGRATION.md](MIGRATION.md) for the step-by-step 2.x → 3.0 upgrade guide.

### Added
- Characterization test suite locking the public 2.x behavior (DI lifecycle,
  `GoogleAuthProvider` entry points, sign-in overload defaults, public model shapes).

### Changed
- Build toolchain: Android Gradle Plugin 9.2 (`com.android.kotlin.multiplatform.library`),
  Gradle 9.4.1. **JVM target raised from 1.8 to 17** for the android and jvm
  artifacts — consumers need a Java 17+ runtime for desktop/JVM apps.
- Android artifacts are now published as a **single variant**; the separate
  `debug` variant is no longer published. Debug builds resolve the release
  variant automatically.
- Android host (unit) test task is now `testAndroid` (was
  `testDebugUnitTest`/`testReleaseUnitTest`).
- Kotlin 2.2.21 → **2.4.0**; Compose Multiplatform plugin 1.9.3 → 1.10.3
  (1.11+ drops the `iosX64` target this library still publishes); Ktor 3.5.1,
  kotlinx-serialization 1.11.0, kotlinx-coroutines 1.11.0, Koin 4.2.2 (stable),
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
- _(pending)_ Legacy overloads slated for removal in 4.0 carry
  `@Deprecated(ReplaceWith(...))` with migration hints.

### Removed
- **Koin.** KMPAuth no longer uses or ships Koin. The `@KMPAuthInternalApi`
  DI types `KMPKoinComponent` and `LibDependencyInitializer` are deleted and
  the `io.insert-koin:koin-core` dependency is gone from every module.
  Public entry points (`GoogleAuthProvider.create`, all `*UiContainer`
  composables) are unchanged — internal wiring is now plain constructor
  injection. Only code that referenced those two internal-API types directly
  is affected (see MIGRATION.md).
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
