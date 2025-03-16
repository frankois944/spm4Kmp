// swift-tools-version: 5.9
import PackageDescription

let package = Package(
  name: "exportedNativeMacosShared",
  platforms: [.iOS("12.0"), .macOS("10.13"), .tvOS("12.0"), .watchOS("4.0")],
  products: [
    .library(
      name: "exportedNativeMacosShared",
      type: .static,
      targets: ["exportedNativeMacosShared"])
  ],
  dependencies: [
    .package(
      path:
        "/Users/francoisdabonot/devs/spm4Kmp/example/../plugin-build/plugin/src/functionalTest/resources/LocalSourceDummyFramework"
    )
  ],
  targets: [
    .target(
      name: "exportedNativeMacosShared",
      dependencies: [
        .product(name: "LocalSourceDummyFramework", package: "LocalSourceDummyFramework")
      ],
      path: "Sources"

    )

  ]
)
