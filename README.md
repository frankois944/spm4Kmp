<img src="docs/assets/img.png" alt="spmForKmp logo" style="width:150px; height:auto;">

# The Swift Package Manager for Kotlin multiplatform Plugin

[![Gradle Plugin Portal Version](https://img.shields.io/gradle-plugin-portal/v/io.github.frankois944.spmForKmp)](https://plugins.gradle.org/plugin/io.github.frankois944.spmForKmp)
[![build&tests](https://github.com/frankois944/spm4Kmp/actions/workflows/pre-merge.yaml/badge.svg)](https://github.com/frankois944/spm4Kmp/actions/workflows/pre-merge.yaml)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=frankois944_spm4Kmp&metric=coverage)](https://sonarcloud.io/summary/new_code?id=frankois944_spm4Kmp)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=frankois944_spm4Kmp&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=frankois944_spm4Kmp)
[![GitHub License](https://img.shields.io/github/license/frankois944/spm4kmp)](https://github.com/frankois944/spm4Kmp/blob/main/LICENSE)

The Swift Package Manager for Kotlin Multiplatform Plugin (`spmForKmp`) is a modern **alternative to the [deprecated](https://blog.cocoapods.org/CocoaPods-Specs-Repo/) CocoaPods Plugin** previously used by the [KMP CocoaPods plugin](https://kotlinlang.org/docs/native-cocoapods.html#set-up-an-environment-to-work-with-cocoapods).

It helps you integrate Swift Packages and simplifies communication between Swift and Kotlin Multiplatform projects targeting **Apple platforms**.

You can also easily **use your own Swift code** in your Kotlin Apple source sets!

The plugin uses the native Swift Package Manager, so **no third-party dependencies are required**, making it less intrusive and more reliable.

# [READ THE DOCUMENTATION](https://spmforkmp.eu/)

# QuickStart

## Gradle Plugin

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
    ).forEach { target ->
        // `cinteropName` is recommended when using a list of native target
        // Or when you want to keep the compatibility with the legacy way (cf: [cinteropName])
        // If not set, It will take the name of the current Target
        target.swiftPackageConfig(cinteropName = "[cinteropName]") {
            // create a new directory at `src/swift/[cinteropName]`
        }
    }
}
```

### ⭐️ Support My Project ⭐️

If you find this project useful, please consider giving it a star!

[![GitHub stars](https://img.shields.io/github/stars/frankois944/spm4Kmp?style=social)](https://github.com/frankois944/spm4Kmp)

[![ko-fi](https://ko-fi.com/img/githubbutton_sm.svg)](https://ko-fi.com/A0A51MG1Y9)

## Star History

[![Star History Chart](https://api.star-history.com/svg?repos=frankois944/spm4Kmp&type=date&legend=top-left)](https://www.star-history.com/#frankois944/spm4Kmp&type=date&legend=top-left)

---

For additional help or detailed documentation, refer to the official repository or contact the plugin maintainers. Happy coding! 🎉
