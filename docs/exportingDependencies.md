# Export Dependencies to Kotlin

Building on [using external dependencies](bridgeWithDependencies.md), you can export compatible packages directly to your Kotlin module — no bridge wrapping needed.

!!! note
    If a package doesn't work with the plugin, please [open an issue](https://github.com/frankois944/spm4Kmp/issues).

---

## How It Works

When a dependency has an Objective-C compatible interface, it can be exported directly via `exportToKotlin = true`. The plugin runs `cinterop` over the package's generated Objective-C header and makes its types available in your Kotlin code.

!!! warning "Only export ObjC-compatible packages"
    Exporting a [pure Swift package](./section-help/faq.md#when-exporting-a-product-i-have-only-swift_typedefs-or-swift_-available-in-my-kotlin-code) produces only empty `SWIFT_TYPEDEFS` in Kotlin and wastes build time. If the package is pure Swift, wrap it in a [bridge](bridgeWithDependencies.md#example) instead.

---

## Example

Exporting [FirebaseAnalytics](https://github.com/firebase/firebase-ios-sdk) — an ObjC-compatible library — directly to Kotlin.

### Gradle

```kotlin title="build.gradle.kts"
kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64()
        // and more Apple targets...
    ).forEach { target ->
        target.swiftPackageConfig(cinteropName = "[cinteropName]") {
            dependency {
                remotePackageVersion(
                    url = uri("https://github.com/firebase/firebase-ios-sdk"),
                    version = "11.8.0",
                    products = {
                        add("FirebaseAnalytics", exportToKotlin = true) // exported
                        add("FirebaseCore")                              // not exported
                    },
                )
                // more dependencies...
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
                version = "11.8.0",
                products = {
                    add("FirebaseAnalytics", exportToKotlin = true)
                    add("FirebaseCore")
                },
            )
        }
    }
}
```

</details>

### Kotlin Usage

```kotlin title="iosMain/kotlin/com/example/myKotlinFile.kt"
import FirebaseAnalytics.FIRConsentStatusGranted

@ExperimentalForeignApi
val consentStatusGranted = FIRConsentStatusGranted
```

!!! note
    The bridge file can remain empty — no Swift wrapping is needed when exporting an ObjC-compatible product directly.
