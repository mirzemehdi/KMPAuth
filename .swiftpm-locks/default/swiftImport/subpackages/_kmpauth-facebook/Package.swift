// swift-tools-version: 5.9
import PackageDescription
let package = Package(
  name: "_kmpauth-facebook",
  platforms: [
    .iOS("16.0")
  ],
  products: [
    .library(
      name: "_kmpauth-facebook",
      type: .none,
      targets: ["_kmpauth-facebook"]
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
      name: "_kmpauth-facebook",
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
