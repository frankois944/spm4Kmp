# Export Dependencies To Kotlin

## How It works

On completion with [using external dependencies](bridgeWithDependencies.md), it's possible to export them to Kotlin, if they are [compatible](faq.md#when-exporting-a-product-i-have-only-swift_typedefs-or-swift_-available-in-my-kotlin-code).

Exported dependency can be used inside the bridge, the Swift & Kotlin App.

!!! note

    If your package doesn't work with the plugin, please create an [issue](https://github.com/frankois944/spm4Kmp/issues).

### Bridge Incompatible Dependencies

In a case the exported dependency is written in [Swift](faq.md), **manual work needs to be done** like [this](bridgeWithDependencies.md#example).

For example, the [CryptoSwift](https://github.com/krzyzanowskim/CryptoSwift) can't work directly on Kotlin, so the Plugin's bridge is here to fill the hole between Kotlin and Swift.

## Gradle

The following configuration export to Kotlin the package [FirebaseAnalytics](https://github.com/firebase/firebase-ios-sdk) which is a ObjC library.

!!! warning "Don't export incompatible library"

    Exporting an incompatible library is useless and will only increase build time.

``` kotlin title="build.gradle.kts"
swiftPackageConfig {
    create("[cinteropName]") {
        dependency(
            SwiftDependency.Package.Remote.Version(
                url = URI("https://github.com/firebase/firebase-ios-sdk.git"),
                products = {
                    add("FirebaseAnalytics", exportToKotlin = true), // exported
                    add("FirebaseCore") // non-exported
                },
                version = "11.8.0",
            ),
            // Another SwiftDependency
            // ...
        )
    }
}
```

!!! warning

    A local swift package is being generated during the build and this message diplayed
    ```
    Spm4Kmp: A local Swift package has been generated at
    /path/to/the/local/package
    Please add it to your xcode project as a local package dependency.
    ```
    Add the folder to your Xcode project as a Local package, that's all.

    Note : When updating your configuration, reset the package cache to apply the modification.

## Example

``` kotlin title="iosMain/kotlin/com/example/myKotlinFile.kt"
import FirebaseAnalytics.FIRConsentStatusGranted

@ExperimentalForeignApi
val consentStatusGranted = FIRConsentStatusGranted

```

!!! note

    The bridge can remain empty as we don't need it; we only want to use the exported product.
