// swift-tools-version: 5.9
import PackageDescription
let package = Package(
  name: "_providers_kmpauth_facebook",
  platforms: [
    .iOS("16.0")
  ],
  products: [
    .library(
      name: "_providers_kmpauth_facebook",
      type: .none,
      targets: ["_providers_kmpauth_facebook"]
    )
  ],
  dependencies: [
    .package(
      url: "https://github.com/facebook/facebook-ios-sdk.git",
      from: "18.0.0"
    )
  ],
  targets: [
    .target(
      name: "_providers_kmpauth_facebook",
      dependencies: [
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
