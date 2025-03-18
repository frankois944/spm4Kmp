# Getting Started

## Requirement

- macOS With Xcode 16 and later
    - Using an earlier version of Xcode is possible, but I don't recommend it.
- Kotlin : **2.1.0 and later**
- Gradle : **8.10 and later**



## Plugins

[![Gradle Plugin Portal Version](https://img.shields.io/gradle-plugin-portal/v/io.github.frankois944.spmForKmp)](https://plugins.gradle.org/plugin/io.github.frankois944.spmForKmp)

``` kotlin title="build.gradle.kts"
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

``` kotlin title="build.gradle.kts"
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

[swiftPackageConfig reference](./references/swiftPackageConfig.md)
``` kotlin title="build.gradle.kts"
swiftPackageConfig {
    create("[cinteropName]") { // must match with cinterops.create name
    }
}
```


