# Getting Started

## Requirements

| Tool            | Minimum version |
| --------------- |-----------------|
| macOS + Xcode   | 16+             |
| Kotlin          | 2.2.20+         |
| Gradle          | 8.12+           |

!!! tip
    Using an earlier Xcode version is possible — see [Custom Swift Versions & Toolchains](section-help/tips.md#support-xcode-15-and-earlier-or-another-version-of-swift). Always use the latest Kotlin version when possible.

---

## Apply the Plugin

[![Gradle Plugin Portal Version](https://img.shields.io/gradle-plugin-portal/v/io.github.frankois944.spmForKmp)](https://plugins.gradle.org/plugin/io.github.frankois944.spmForKmp)

```kotlin title="build.gradle.kts"
plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("io.github.frankois944.spmForKmp") version "[version]"
}
```

---

## Gradle Properties

Add the following to your `gradle.properties`:

```properties title="gradle.properties"
kotlin.mpp.enableCInteropCommonization=true
```

---

## Initial Configuration

=== "Multiple targets (recommended)"

    Use `cinteropName` to share a single bridge across targets and maintain compatibility with the legacy configuration style.

    ```kotlin title="build.gradle.kts"
    kotlin {
        listOf(
            iosArm64(),
            iosSimulatorArm64()
            // and more Apple targets...
        ).forEach { target ->
            target.swiftPackageConfig(cinteropName = "[cinteropName]") {
                // creates src/swift/[cinteropName]/
            }
        }
    }
    ```

=== "Single target"

    When configuring a single target, `cinteropName` is optional — the target name is used by default.

    ```kotlin title="build.gradle.kts"
    kotlin {
        iosArm64 {
            swiftPackageConfig {
                // creates src/swift/iosArm64/
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
                cinterops.create("[cinteropName]")
            }
        }
    }
}
```

```kotlin title="build.gradle.kts"
swiftPackageConfig {
    create("[cinteropName]") { // must match cinterops.create name
    }
}
```

</details>

See the full [swiftPackageConfig reference](references/swiftPackageConfig.md) for all available options.
