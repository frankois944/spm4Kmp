// swift-tools-version: 5.9
import PackageDescription

let package = Package(
  name: "exportedNativeIosShared",
  platforms: [.iOS("12.0"), .macOS("10.13"), .tvOS("12.0"), .watchOS("4.0")],
  products: [
    .library(
      name: "exportedNativeIosShared",
      type: .static,
      targets: ["exportedNativeIosShared", "DummyFramework"])
  ],
  dependencies: [
    .package(url: "https://github.com/krzyzanowskim/CryptoSwift.git", exact: "1.8.1"),
    .package(url: "https://github.com/firebase/firebase-ios-sdk.git", exact: "11.8.1"),
    .package(
      path:
        "/Users/francoisdabonot/devs/spm4Kmp/example/../plugin-build/plugin/src/functionalTest/resources/LocalSourceDummyFramework"
    ), .package(url: "https://github.com/krzyzanowskim/CryptoSwift.git", exact: "1.8.1"),
    .package(url: "https://github.com/appmetrica/appmetrica-sdk-ios", exact: "5.0.0"),
  ],
  targets: [
    .target(
      name: "exportedNativeIosShared",
      dependencies: [
        .product(name: "CryptoSwift", package: "CryptoSwift"),
        .product(name: "FirebaseCore", package: "firebase-ios-sdk"),
        .product(name: "FirebaseAnalytics", package: "firebase-ios-sdk"),
        .product(name: "FirebaseDatabase", package: "firebase-ios-sdk"), "DummyFramework",
        .product(name: "LocalSourceDummyFramework", package: "LocalSourceDummyFramework"),
        .product(name: "CryptoSwift", package: "CryptoSwift"),
        .product(name: "AppMetricaCore", package: "appmetrica-sdk-ios"),
      ],
      path: "Sources"

    ),
    .binaryTarget(
      name: "DummyFramework",
      path: "../../plugin-build/plugin/src/functionalTest/resources/DummyFramework.xcframework.zip"),
  ]
)
