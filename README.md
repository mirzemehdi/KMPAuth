# KMPNotifier - Kotlin Multiplatform Authentication Library
[![Build](https://github.com/mirzemehdi/KMPAuth/actions/workflows/build.yml/badge.svg)](https://github.com/mirzemehdi/KMPAuth/actions/workflows/build.yml)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.21-blue.svg?style=flat&logo=kotlin)](https://kotlinlang.org)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.mirzemehdi/kmpauth-google?color=blue)](https://search.maven.org/search?q=g:io.github.mirzemehdi+kmpauth)

![badge-android](http://img.shields.io/badge/platform-android-6EDB8D.svg?style=flat)
![badge-ios](http://img.shields.io/badge/platform-ios-CDCDCD.svg?style=flat)

Simple and easy to use Kotlin Multiplatform Authentication library targeting ios and android. 
This library is developed based on this [blog post](https://proandroiddev.com/integrating-google-sign-in-into-kotlin-multiplatform-8381c189a891) 
and for Now Only Google Sign-In feature is implemented, but it is planned to add more authentication integrations.
You can check out [Documentation](https://mirzemehdi.github.io/KMPAuth) for full library api information.

## Features
- ✅ Google One Tap Sign-In
- 🚧 Apple Sign-In (Not implemented yet, In progress)
- 🚧 Facebook Sign-In (Not implemented yet, In progress)
- 📱 Multiplatform (android and iOS)

## Installation
For Google Sign-In, you need to set up OAuth 2.0 in Google Cloud Platform Console. 
For steps you can follow this [link](https://support.google.com/cloud/answer/6158849). **_Pro Easy Tip:_** If you use Firebase and enable Google Sign-In authentication in Firebase 
it will automatically generate OAuth client IDs for each platform, 
and one will be **_Web Client ID_** which will be needed for identifying signed-in users in backend server.

### Gradle Setup
KMPAuth is available on Maven Central. In your root project `build.gradle.kts` file (or `settings.gradle` file) add `mavenCentral()` to repositories.

```kotlin
repositories { 
  mavenCentral()
}
```

Then in your shared module you add dependency in `commonMain`. Latest version: [![Maven Central](https://img.shields.io/maven-central/v/io.github.mirzemehdi/kmpauth-google?color=blue)](https://search.maven.org/search?q=g:io.github.mirzemehdi+kmpauth).
```kotlin
sourceSets {
  commonMain.dependencies {
    api("io.github.mirzemehdi:kmpauth-google:<version>")
  }
}
```

### Platform Setup

<details>
  <summary>Android</summary>

### Android Setup
There is not any platform specific setup in Android side.

</details>

<details>
  <summary>iOS</summary>

### iOS Setup
First, you need to include Google Sign-In library to your ios app using Swift Package Manager or Cocoapods. 
Then add clientID, and serverId to your `Info.plist` file as below:

```
<key>GIDServerClientID</key>
<string>YOUR_SERVER_CLIENT_ID</string>

<key>GIDClientID</key>
<string>YOUR_IOS_CLIENT_ID</string>
<key>CFBundleURLTypes</key>
<array>
  <dict>
    <key>CFBundleURLSchemes</key>
    <array>
      <string>YOUR_DOT_REVERSED_IOS_CLIENT_ID</string>
    </array>
  </dict>
</array>

```

And finally, you need the code below to implement application delegate function calls on the Swift side.

```swift
import SwiftUI
import shared
import GoogleSignIn

class AppDelegate: NSObject, UIApplicationDelegate {

    func application(
      _ app: UIApplication,
      open url: URL, options: [UIApplication.OpenURLOptionsKey : Any] = [:]
    ) -> Bool {
      var handled: Bool

      handled = GIDSignIn.sharedInstance.handle(url)
      if handled {
        return true
      }

      // Handle other custom URL types.

      // If not handled by this app, return false.
      return false
    }


}

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate
    
   var body: some Scene {
      WindowGroup {
            ContentView().onOpenURL(perform: { url in
                GIDSignIn.sharedInstance.handle(url)
            })
      }
   }
}


```

</details>

## Usage
Create GoogleAuthProvider instance.
```kotlin
val googleAuthProvider = GoogleAuthProvider.create(credentials = GoogleAuthCredentials(serverId = "WEB_CLIENT_ID"))
```

### Google Sign-In
Google Sign-In is UI process. You can either use below function in your `@Composable` function, or 
you can directly use `GoogleButtonUiContainer` which handles all complex stuff for you.

```kotlin
val googleAuthUiProvider = googleAuthProvider.getUiProvider()
val googleUser = googleAuthUiProvider.signIn() //suspend function, needs to be called in CoroutineScope
```

or using `GoogleButtonUiContainer`. Make sure you create GoogleAuthProvider instance before invoking below composable function.
```kotlin
GoogleButtonUiContainer(onGoogleSignInResult = { googleUser ->
    val idToken=googleUser?.idToken // Send this idToken to your backend to verify
}) {
    Button(
        onClick = { this.onClick() } //Delegate button or any view click to GoogleButtonUiContainer click method
    ) {
        Text("Sign-In with Google")
    }
}
```
### Google Sign out
Since it is not UI related function you can sign out user in any part of code.
```kotlin
googleAuthProvider.signOut()
```




