# Facebook Sign-In

Module: `kmpauth-facebook`. Android and iOS only (the Facebook SDK has no
other targets).

## Usage

```kotlin
// Credential only:
val facebookSignIn = rememberFacebookSignInState(onResult = { result: Result<FacebookUser> -> })

// Or a full session through the registered backend:
val facebookAuth = rememberFacebookAuthState(onResult = { result: Result<KMPAuthUser> -> })
FacebookSignInButton { facebookAuth.launch() }
```

## Login tracking (token type)

Both states accept `loginTracking`, consistent on Android and iOS:

- `FacebookLoginTracking.Limited` (**default**) — privacy-friendly Limited
  Login; returns an OIDC JWT + nonce (no iOS App Tracking Transparency
  prompt). Firebase exchanges it through the OIDC OAuth provider.
- `FacebookLoginTracking.Enabled` — classic login; returns a Graph-API
  access token in `FacebookUser.accessToken`. Counts as tracking on iOS
  (handle ATT).

```kotlin
// If your backend needs a Graph-API access token:
val facebookSignIn = rememberFacebookSignInState(
    loginTracking = FacebookLoginTracking.Enabled,
    onResult = { result -> val accessToken = result.getOrNull()?.accessToken },
)
```

## Android setup

Add to `res/values/strings.xml`:

```xml
<string name="facebook_app_id">YOUR_FACEBOOK_APP_ID</string>
<string name="fb_login_protocol_scheme">fbYOUR_FACEBOOK_APP_ID</string>
<string name="facebook_client_token">YOUR_FACEBOOK_CLIENT_TOKEN</string>
```

Add to `AndroidManifest.xml` inside `<application>`:

```xml
<meta-data
    android:name="com.facebook.sdk.ApplicationId"
    android:value="@string/facebook_app_id" />

<meta-data
    android:name="com.facebook.sdk.ClientToken"
    android:value="@string/facebook_client_token" />

<activity
    android:name="com.facebook.FacebookActivity"
    android:configChanges="keyboard|keyboardHidden|screenLayout|screenSize|orientation"
    android:label="@string/app_name" />
```

When using `FacebookLoginTracking.Limited` (the **default**), forward your
main Activity's result to `KMPAuth.handleFacebookActivityResult`:

```kotlin
override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    KMPAuth.handleFacebookActivityResult(requestCode, resultCode, data)
    super.onActivityResult(requestCode, resultCode, data)
}
```

> **Why only for `Limited`?** Limited Login requires a nonce, which the
> Facebook SDK accepts only through `LoginConfiguration` — an API whose
> result still arrives via `onActivityResult`. With
> `FacebookLoginTracking.Enabled`, KMPAuth uses the SDK's AndroidX Activity
> Result API and **no override is needed**. Calling
> `handleFacebookActivityResult` when it isn't needed is harmless.

## iOS setup

Add the Facebook SDK Swift package, then add to `Info.plist`:

```xml
<key>CFBundleURLTypes</key>
<array>
  <dict>
    <key>CFBundleURLSchemes</key>
    <array>
      <string>fbFACEBOOK_APP_ID</string> <!-- Your Facebook App ID with 'fb' prefix -->
    </array>
  </dict>
</array>

<key>FacebookAppID</key>
<string>FACEBOOK_APP_ID</string>

<key>FacebookClientToken</key>
<string>YOUR_FACEBOOK_CLIENT_TOKEN</string>

<key>FacebookDisplayName</key>
<string>YourAppDisplayName</string>

<key>LSApplicationQueriesSchemes</key>
<array>
  <string>fbapi</string>
  <string>fb-messenger-api</string>
  <string>fbauth2</string>
  <string>fbshareextension</string>
</array>
```

Initialize the Facebook SDK on the Swift side:

```swift
func application(_ application: UIApplication,
                 didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil) -> Bool {
    FirebaseApp.configure()
    FBSDKCoreKit.ApplicationDelegate.shared.application(
        application,
        didFinishLaunchingWithOptions: launchOptions
    )
    return true
}

func application(
    _ app: UIApplication,
    open url: URL,
    options: [UIApplication.OpenURLOptionsKey : Any] = [:]
) -> Bool {
    return FBSDKCoreKit.ApplicationDelegate.shared.application(
        app,
        open: url,
        options: options
    )
}
```

## Reference docs

- [Facebook Login for Android](https://developers.facebook.com/docs/facebook-login/android)
- [Facebook Login for iOS](https://developers.facebook.com/docs/facebook-login/ios)
- [Firebase Authentication with Facebook](https://firebase.google.com/docs/auth/android/facebook-login)
