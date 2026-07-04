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
- _(pending)_ Build toolchain: AGP 9, Gradle 9.4, Kotlin 2.4, JVM target 17.
- _(pending)_ iOS dependencies distributed via Swift Package Manager instead of
  CocoaPods; iOS minimum deployment target raised to 16.0.

### Deprecated
- _(pending)_ Legacy overloads slated for removal in 4.0 carry
  `@Deprecated(ReplaceWith(...))` with migration hints.

### Removed
- _(pending)_ Koin-based internal DI (`KMPKoinComponent`, `LibDependencyInitializer`,
  `org.koin` dependency). These were `@KMPAuthInternalApi`-annotated internals.

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
