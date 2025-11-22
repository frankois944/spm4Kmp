# Export Dependencies To Kotlin

## How It works

On completion with [using external dependencies](bridgeWithDependencies.md), it's possible to export them to your Kotlin module, if they are [compatible](./section-help/faq.md#when-exporting-a-product-i-have-only-swift_typedefs-or-swift_-available-in-my-kotlin-code).

!!! note

    If your package doesn't work with the plugin, please create an [issue](https://github.com/frankois944/spm4Kmp/issues).

### Bridge Incompatible Dependencies

In a case the exported dependency is written in [Swift](./section-help/faq.md), **manual work needs to be done** like [this](bridgeWithDependencies.md#example).

For example, the [CryptoSwift](https://github.com/krzyzanowskim/CryptoSwift) can't work directly on Kotlin, so the Plugin's bridge is here to fill the gape between Kotlin and Swift.

## Example

### Gradle

The following configuration exports to Kotlin the package [FirebaseAnalytics](https://github.com/firebase/firebase-ios-sdk) which is a ObjC library.

!!! warning "Don't export incompatible library"

    Exporting an incompatible library is useless and will only increase build time.

```kotlin title="build.gradle.kts"
kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64()
        // and more Apple targets...
    ).forEach { target ->
        target.swiftPackageConfig(groupName = "[cinteropName]") {
            dependency {
                remotePackageVersion(
                    url = uri("https://github.com/firebase/firebase-ios-sdk"),
                    products = {
                        add("FirebaseAnalytics", exportToKotlin = true), // exported
                        add("FirebaseCore") // non-exported
                    },
                    version = "11.8.0",
                )
                // Another SwiftDependency
                // ...
            }
        }
    }
}
```

<details>
<summary>Legacy (< 1.1.0)</summary>
```kotlin title="build.gradle.kts"
swiftPackageConfig {
    create("[cinteropName]") {
        dependency {
            remotePackageVersion(
                url = uri("https://github.com/firebase/firebase-ios-sdk"),
                products = {
                    add("FirebaseAnalytics", exportToKotlin = true), // exported
                    add("FirebaseCore") // non-exported
                },
                version = "11.8.0",
            )
            // Another SwiftDependency
            // ...
        }
    }
}
```
</details>

### Bridge

```kotlin title="iosMain/kotlin/com/example/myKotlinFile.kt"
import FirebaseAnalytics.FIRConsentStatusGranted

@ExperimentalForeignApi
val consentStatusGranted = FIRConsentStatusGranted

```

!!! note

    The bridge can remain empty as we don't need it; we only want to use the exported product.
