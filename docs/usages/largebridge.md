# Working With Large Bridge

If you plan to have a large bridge for using a particular Pure Swift Package, like Stripe, working with an editor without code completion can be difficult.

The solution is to work with a **local package** you will add to your plugin configuration.

**The big advantage is that you can test your code before adding it to your KMP project**.

## Create A Local Package

- From command line : `swift package init --name YouPackageName`
- From Xcode : File -> New -> Package -> Library

## Choose Your Editor

You can either use **Xcode** or **VSCode with the Swift Plugin**; both are fine.

## Package Manifest

A Swift Package is based on a Manifest, the Package.swift ([official documentation](https://docs.swift.org/package-manager/PackageDescription/PackageDescription.html))

### Example

```swift title="Package.swift"
// swift-tools-version: 5.9
// The swift-tools-version declares the minimum version of Swift required to build this package.

import PackageDescription

let package = Package(
    name: "MyStripeSDK",
    platforms: [.iOS(.v14)],
    products: [
        // Products define the executables and libraries a package produces, making them visible to other packages.
        .library(
            name: "MyStripeSDK",
            targets: ["MyStripeSDK"]),
    ],
    dependencies: [
        .package(url: "https://github.com/stripe/stripe-ios-spm", .upToNextMajor(from: "24.5.0")),
    ],
    targets: [
        // Targets are the basic building blocks of a package, defining a module or a test suite.
        // Targets can depend on other targets in this package and products from dependencies.
        .target(
            name: "MyStripeSDK",
            dependencies: [
                .product(name: "Stripe", package: "stripe-ios-spm"),
                .product(name: "StripePaymentSheet", package: "stripe-ios-spm")
            ]),
        .testTarget(
            name: "MyStripeSDKTests",
            dependencies: [
                "MyStripeSDK"
            ]
        ),
    ]
)
```

## Package Source

Usually located at `Sources/[packageName]`, it has the same requirement as the plugin's bridge source files.

!!! warning "Make your Swift code compatible with Kotlin."

    Your Swift code needs to be marked as [@objc/@objcMembers](https://www.hackingwithswift.com/example-code/language/what-is-the-objcmembers-attribute) and the visibility set as `public`
    or it won't be exported and available from your Kotlin code.

### Example

```swift title="Sources/MyStripeSDK/Package.swift"

import StripePaymentSheet

@objcMembers public class MyStripeSDK: NSObject {

    private var paymentSheet: PaymentSheet?
    private var paymentIntentClientSecret: String
    private let backendCheckoutUrl = URL(string: "Your backend endpoint/payment-sheet")

    public init(paymentIntentClientSecret: String) {
        self.paymentIntentClientSecret = paymentIntentClientSecret
    }

    public func doStripeJob() {
        var configuration = PaymentSheet.Configuration()
        configuration.merchantDisplayName = "Example, Inc."
        self.paymentSheet = PaymentSheet(paymentIntentClientSecret: self.paymentIntentClientSecret,
                                         configuration: configuration)
    }
}
```

## Plugin Configuration

Add your local package to your plugin configuration, follow the [guide](../exportingDependencies.md).

### Example

```kotlin title="build.gradle.kts"

kotlin {
    iosArm64 {
        swiftPackageConfig {
            localPackage(
                // absolute path to your Local Package
                path = "$projectDir/../MyStripeSDK",
                packageName = "MyStripeSDK",
                products = {
                    // Export to Kotlin for use in shared Kotlin code, false by default
                    add("MyStripeSDK", exportToKotlin = true)
                }
            )
        }
    }
}
```

## Don't Store Sensitive Data

!!! warning "The local package source is visible"

    If you share this package, don't put sensitive data inside, as it needs to be [added to the Xcode project](https://frankois944.github.io/spm4Kmp/bridgeWithDependencies/#gradle).

You can use the default plugin's bridge if needed, as it's not visible from the application or your shared Kotlin library.
