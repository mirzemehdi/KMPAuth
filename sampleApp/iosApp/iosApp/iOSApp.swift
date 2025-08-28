import SwiftUI
import composeApp
import GoogleSignIn
import FirebaseCore
import FBSDKCoreKit

class AppDelegate: NSObject, UIApplicationDelegate {

    func application(_ application: UIApplication,
                     didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil) -> Bool {
        FirebaseApp.configure()
        // Initialize Facebook SDK. Maybe move this to KMPAuth later
        FBSDKCoreKit.ApplicationDelegate.shared.application(
            application,
            didFinishLaunchingWithOptions: launchOptions
        )
        AppInitializer.shared.onApplicationStart()

        return true
    }

    func application(
        _ app: UIApplication,
        open url: URL, options: [UIApplication.OpenURLOptionsKey: Any] = [:]
    ) -> Bool {

        var handled: Bool

        handled = GIDSignIn.sharedInstance.handle(url)
        if handled {
            return true
        }

        handled = FBSDKCoreKit.ApplicationDelegate.shared.application(
            app,
            open: url,
            options: options
        )

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
