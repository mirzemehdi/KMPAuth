// swift-tools-version: 5.9
import PackageDescription
let package = Package(
  name: "_backends_firebase_kmpauth-firebase",
  platforms: [
    .iOS("16.0")
  ],
  products: [
    .library(
      name: "_backends_firebase_kmpauth-firebase",
      type: .none,
      targets: ["_backends_firebase_kmpauth-firebase"]
    )
  ],
  dependencies: [
    .package(
      url: "https://github.com/firebase/firebase-ios-sdk.git",
      "11.8.0"..."12.999.999"
    )
  ],
  targets: [
    .target(
      name: "_backends_firebase_kmpauth-firebase",
      dependencies: [
        .product(
          name: "FirebaseAuth",
          package: "firebase-ios-sdk"
        ),
        .product(
          name: "FirebaseCore",
          package: "firebase-ios-sdk"
        )
      ]
    )
  ]
)
