// swift-tools-version: 5.9
import PackageDescription
let package = Package(
  name: "_sampleApp_composeApp",
  platforms: [
    .iOS("16.0")
  ],
  products: [
    .library(
      name: "_sampleApp_composeApp",
      type: .none,
      targets: ["_sampleApp_composeApp"]
    )
  ],
  dependencies: [
  ],
  targets: [
    .target(
      name: "_sampleApp_composeApp",
      dependencies: [
      ]
    )
  ]
)
