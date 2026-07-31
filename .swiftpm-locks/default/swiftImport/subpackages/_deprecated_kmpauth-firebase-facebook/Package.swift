// swift-tools-version: 5.9
import PackageDescription
let package = Package(
  name: "_deprecated_kmpauth-firebase-facebook",
  platforms: [
    .iOS("16.0")
  ],
  products: [
    .library(
      name: "_deprecated_kmpauth-firebase-facebook",
      type: .none,
      targets: ["_deprecated_kmpauth-firebase-facebook"]
    )
  ],
  dependencies: [
    .package(
      url: "https://github.com/firebase/firebase-ios-sdk.git",
      "11.8.0"..."12.999.999"
    ),
    .package(
      url: "https://github.com/facebook/facebook-ios-sdk.git",
      from: "18.0.0"
    )
  ],
  targets: [
    .target(
      name: "_deprecated_kmpauth-firebase-facebook",
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
          name: "FacebookCore",
          package: "facebook-ios-sdk"
        ),
        .product(
          name: "FacebookLogin",
          package: "facebook-ios-sdk"
        )
      ]
    )
  ]
)
