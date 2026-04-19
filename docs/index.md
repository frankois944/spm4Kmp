# Swift Package Manager For Kotlin Multiplatform

[![Gradle Plugin Portal Version](https://img.shields.io/gradle-plugin-portal/v/io.github.frankois944.spmForKmp)](https://plugins.gradle.org/plugin/io.github.frankois944.spmForKmp)
[![build&tests](https://github.com/frankois944/spm4Kmp/actions/workflows/pre-merge.yaml/badge.svg)](https://github.com/frankois944/spm4Kmp/actions/workflows/pre-merge.yaml)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=frankois944_spm4Kmp&metric=coverage)](https://sonarcloud.io/summary/new_code?id=frankois944_spm4Kmp)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=frankois944_spm4Kmp&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=frankois944_spm4Kmp)
[![GitHub License](https://img.shields.io/github/license/frankois944/spm4kmp)](https://github.com/frankois944/spm4Kmp/blob/main/LICENSE)

The Swift Package Manager for Kotlin Multiplatform Plugin (`spmForKmp`) is a modern **alternative to the [deprecated](https://blog.cocoapods.org/CocoaPods-Specs-Repo/) CocoaPods Plugin** previously used by the [KMP CocoaPods plugin](https://kotlinlang.org/docs/native-cocoapods.html#set-up-an-environment-to-work-with-cocoapods).

It helps you integrate Swift Packages and simplifies communication between Swift and Kotlin Multiplatform projects targeting **Apple platforms**.

The plugin uses the native Swift Package Manager, so **no third-party dependencies are required**, making it less intrusive than CocoaPods.

!!! warning "Please Be Aware"

    [Pure Swift packages cannot be exported directly to Kotlin](./section-help/faq.md#whats-a-pure-swift-package); the plugin helps you create a bridge to resolve this.

    While this requires a manual step, it is currently the most effective way to integrate Swift code until native Swift-import is supported in KMP.


## Features

- **Create a Swift↔Kotlin bridge**: Import your own Swift code for functionality that can't be done in Kotlin.
- **Use SPM third-Party Dependency**: Add external dependency and use it inside your bridge
- **Import Swift-compatible code to Kotlin**: Enable SPM dependencies and your own Swift code to be exposed directly in your Kotlin code ([if compatible](./section-help/faq.md#when-exporting-a-product-i-have-only-swift_typedefs-or-swift_-available-in-my-kotlin-code)).

## Swift-Import Playground

[Take a look on the playground](./section-help/tips.md#how-can-i-import-swift-code-into-my-kotlin-code) to understand how you can import Swift code into your kotlin code.

## Support My Project ⭐️

If you find this project useful, please consider giving it a star or a coffee!

[![ko-fi](https://ko-fi.com/img/githubbutton_sm.svg)](https://ko-fi.com/A0A51MG1Y9)

[![GitHub stars](https://img.shields.io/github/stars/frankois944/spm4Kmp?style=social)](https://github.com/frankois944/spm4Kmp)


## Feedback

This project greatly needs feedback, feature requests, and information about the edge case for progressing; let's [talk](https://github.com/frankois944/spm4Kmp/discussions).

## Example

A [sample](https://github.com/frankois944/spm4Kmp/tree/main/example) is available for those who want to see the plugin in action.
