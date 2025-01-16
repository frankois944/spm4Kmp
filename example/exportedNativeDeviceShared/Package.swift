// swift-tools-version: 5.9
import PackageDescription

let package = Package(
  name: "exportedNativeDeviceShared",
  platforms: [.iOS("12.0"), .macOS("10.15"), .tvOS("12.0"), .watchOS("4.0")],
  products: [
    .library(
      name: "exportedNativeDeviceShared",
      type: .static,
      targets: ["exportedNativeDeviceShared", "DummyFramework"])
  ],
  dependencies: [
    .package(url: "https://github.com/firebase/firebase-ios-sdk.git", exact: "11.6.0"),
    .package(
      path:
        "/Users/francoisdabonot/DEV/spm-kmp-plugin/example/../plugin-build/plugin/src/functionalTest/resources/LocalSourceDummyFramework"
    ),
  ],
  targets: [
    .target(
      name: "exportedNativeDeviceShared",
      dependencies: [
        .product(name: "FirebaseCore", package: "firebase-ios-sdk"),
        .product(name: "FirebaseAnalytics", package: "firebase-ios-sdk"), "DummyFramework",
        .product(name: "LocalSourceDummyFramework", package: "LocalSourceDummyFramework"),
      ],
      path: "Sources"),
    .binaryTarget(
      name: "DummyFramework",
      path: "../../plugin-build/plugin/src/functionalTest/resources/DummyFramework.xcframework.zip"),
  ]
)
