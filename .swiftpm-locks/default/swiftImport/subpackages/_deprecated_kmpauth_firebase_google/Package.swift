// swift-tools-version: 5.9
import PackageDescription
let package = Package(
  name: "_deprecated_kmpauth_firebase_google",
  platforms: [
    .iOS("16.0")
  ],
  products: [
    .library(
      name: "_deprecated_kmpauth_firebase_google",
      type: .none,
      targets: ["_deprecated_kmpauth_firebase_google"]
    )
  ],
  dependencies: [
    .package(
      url: "https://github.com/firebase/firebase-ios-sdk.git",
      "13.1.1"..."13.999.999"
    ),
    .package(
      url: "https://github.com/google/GoogleSignIn-iOS.git",
      from: "10.1.0"
    )
  ],
  targets: [
    .target(
      name: "_deprecated_kmpauth_firebase_google",
      dependencies: [
        .product(
          name: "FirebaseAuth",
          package: "firebase-ios-sdk"
        ),
        .product(
          name: "FirebaseCore",
          package: "firebase-ios-sdk"
        ),
        .product(
          name: "GoogleSignIn",
          package: "GoogleSignIn-iOS"
        )
      ]
    )
  ]
)
