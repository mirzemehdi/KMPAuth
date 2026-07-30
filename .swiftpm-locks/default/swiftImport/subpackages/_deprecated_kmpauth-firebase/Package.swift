// swift-tools-version: 5.9
import PackageDescription
let package = Package(
  name: "_deprecated_kmpauth-firebase",
  platforms: [
    .iOS("16.0")
  ],
  products: [
    .library(
      name: "_deprecated_kmpauth-firebase",
      type: .none,
      targets: ["_deprecated_kmpauth-firebase"]
    )
  ],
  dependencies: [
  ],
  targets: [
    .target(
      name: "_deprecated_kmpauth-firebase",
      dependencies: [
      ]
    )
  ]
)
