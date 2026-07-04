# Migrating KMPAuth 2.x → 3.0

Step-by-step guide for upgrading. Sections marked _(pending)_ are filled in as
the corresponding 3.0 changes land on the `rel_3.0.0` integration branch.

## TL;DR checklist

- [ ] Bump `io.github.mirzemehdi:kmpauth-*` to `3.0.0`.
- [ ] _(pending)_ iOS: replace CocoaPods pods with Swift Package Manager packages;
      raise your app's iOS deployment target to 16.0 and use Xcode 16.4+.
- [ ] _(pending)_ Desktop/JVM: run on Java 17+ (artifacts now target JVM 17 bytecode).
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

## 1. iOS: CocoaPods → Swift Package Manager _(pending)_

_Instructions land with the SPM migration PR._

## 2. Koin removal _(pending)_

KMPAuth no longer uses (or ships) Koin. Details land with the DI PRs.

## 3. Toolchain requirements _(pending)_

| Requirement | 2.x | 3.0 |
|---|---|---|
| iOS deployment target | 11.0–12.0 | 16.0 _(pending)_ |
| Xcode | 15+ | 16.4+ _(pending)_ |
| JVM bytecode | 1.8 | 17 _(pending)_ |
| Android debug variant artifact | published | no longer published (single-variant; debug builds resolve the release variant automatically) _(pending)_ |

## 4. Pluggable auth backends _(pending)_

3.0 introduces an `AuthProviderBackend` abstraction (Firebase remains the
default implementation; a Supabase implementation is planned). Existing
Firebase-based code keeps working unchanged.
