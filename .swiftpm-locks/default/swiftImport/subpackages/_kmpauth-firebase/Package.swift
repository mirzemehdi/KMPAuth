// swift-tools-version: 5.9
import PackageDescription
let package = Package(
  name: "_kmpauth-firebase",
  platforms: [
    .iOS("16.0")
  ],
  products: [
    .library(
      name: "_kmpauth-firebase",
      type: .none,
      targets: ["_kmpauth-firebase"]
    )
  ],
  dependencies: [
    .package(
      url: "https://github.com/firebase/firebase-ios-sdk.git",
      from: "11.8.0"
    ),
    .package(
      url: "https://github.com/google/GoogleSignIn-iOS.git",
      from: "9.1.0"
    )
  ],
  targets: [
    .target(
      name: "_kmpauth-firebase",
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
