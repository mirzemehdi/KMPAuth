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
  ],
  targets: [
    .target(
      name: "_kmpauth-firebase",
      dependencies: [
      ]
    )
  ]
)
