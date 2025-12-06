# Getting Started

## Requirement

- macOS With Xcode 16 and later
    - Using an earlier version of [Xcode is possible](section-help/tips.md#support-xcode-15-and-earlier-or-another-version-of-swift), but I don't recommend it.
- Kotlin : **2.1.21 and later**
    - It's always recommended to use the latest version of Kotlin.
- Gradle : **8.12 and later**


## Plugins

[![Gradle Plugin Portal Version](https://img.shields.io/gradle-plugin-portal/v/io.github.frankois944.spmForKmp)](https://plugins.gradle.org/plugin/io.github.frankois944.spmForKmp)

```kotlin title="build.gradle.kts"
plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("io.github.frankois944.spmForKmp") version "[version]"
}
```

## Gradle Properties

``` title="gradle.properties"
kotlin.mpp.enableCInteropCommonization=true
```

## Initial Configuration

```kotlin title="build.gradle.kts"
// List of targets
kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64()
        // and more Apple targets...
    ).forEach { target ->
        // `cinteropName` is recommended when using a list of native target
        // Or when you want to keep the compatibility with the legacy way (cf: [cinteropName])
        // If not set, It will take the name of the current Target
        target.swiftPackageConfig(cinteropName = "[cinteropName]") {
            // create a new directory at `src/swift/[cinteropName]`
        }
    }
}

// For a single target

kotlin {
    iosArm64 {
        swiftPackageConfig {
            // create a new directory at `src/swift/iosArm64`
        }
    }
}
```

<details>
<summary>Legacy (< 1.1.0)</summary>
```kotlin title="build.gradle.kts"
kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64()
        // and more Apple targets...
    ).forEach {
        it.compilations {
            val main by getting {
                // Choose the cinterop name
                cinterops.create("[cinteropName]")
            }
        }
    }
}
```

```kotlin title="build.gradle.kts"
swiftPackageConfig {
    create("[cinteropName]") { // must match with cinterops.create name
    }
}
```
</details>

[swiftPackageConfig reference](references/swiftPackageConfig.md)


