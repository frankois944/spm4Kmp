# Export Dependencies To Kotlin

## How It works

On completion with [using external dependencies](bridgeWithDependencies.md), it's possible to export them to your Kotlin module, if they are [compatible](./section-help/faq.md#when-exporting-a-product-i-have-only-swift_typedefs-or-swift_-available-in-my-kotlin-code).

!!! note

    If your package doesn't work with the plugin, please create an [issue](https://github.com/frankois944/spm4Kmp/issues).

### Bridge Incompatible Dependencies

In a case the exported dependency is written in [Swift](./section-help/faq.md), **manual work needs to be done** like [this](bridgeWithDependencies.md#example).

For example, the [CryptoSwift](https://github.com/krzyzanowskim/CryptoSwift) can't work directly on Kotlin, so the Plugin's bridge is here to fill the gape between Kotlin and Swift.

### Local Package

Some _specific_ dependencies **require** to be declared on the Xcode side; in that case, you will see the following message during the build:
```
Spm4Kmp: The following dependencies [some_dependency_name] need to be added to your xcode project
A local Swift package has been generated at
/path/to/the/local/package
Please add it to your xcode project as a local package dependency; it will add the missing content.
****You can ignore this messaging if you have already added these dependencies to your Xcode project****
```

## Gradle

The following configuration exports to Kotlin the package [FirebaseAnalytics](https://github.com/firebase/firebase-ios-sdk) which is a ObjC library.

!!! warning "Don't export incompatible library"

    Exporting an incompatible library is useless and will only increase build time.

``` kotlin title="build.gradle.kts"
swiftPackageConfig {
    create("[cinteropName]") {
        dependency {
            remotePackageVersion(
                url = URI("https://github.com/firebase/firebase-ios-sdk"),
                products = {
                    add("FirebaseAnalytics", exportToKotlin = true), // exported
                    add("FirebaseCore") // non-exported
                },
                version = "11.8.0",
            )
            // Another SwiftDependency
            // ...
        )
    }
}
```

## Example

``` kotlin title="iosMain/kotlin/com/example/myKotlinFile.kt"
import FirebaseAnalytics.FIRConsentStatusGranted

@ExperimentalForeignApi
val consentStatusGranted = FIRConsentStatusGranted

```

!!! note

    The bridge can remain empty as we don't need it; we only want to use the exported product.
