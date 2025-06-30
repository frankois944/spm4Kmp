# Swift Package Manager For Kotlin Multiplatform

[![Gradle Plugin Portal Version](https://img.shields.io/gradle-plugin-portal/v/io.github.frankois944.spmForKmp)](https://plugins.gradle.org/plugin/io.github.frankois944.spmForKmp)
[![build&tests](https://github.com/frankois944/spm4Kmp/actions/workflows/pre-merge.yaml/badge.svg)](https://github.com/frankois944/spm4Kmp/actions/workflows/pre-merge.yaml)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=frankois944_spm4Kmp&metric=coverage)](https://sonarcloud.io/summary/new_code?id=frankois944_spm4Kmp)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=frankois944_spm4Kmp&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=frankois944_spm4Kmp)
[![GitHub License](https://img.shields.io/github/license/frankois944/spm4kmp)](https://github.com/frankois944/spm4Kmp/blob/main/LICENSE)

The Swift Package Manager for Kotlin Multiplatform Plugin, aka `spmForKmp` Gradle Plugin, is an **alternative of the [dying](https://blog.cocoapods.org/CocoaPods-Specs-Repo/) CocoaPods Plugin** used by [KMP cocoapods plugin](https://kotlinlang.org/docs/native-cocoapods.html#set-up-an-environment-to-work-with-cocoapods).

It will help you to integrate Swift Package and simplify communication between Swift/Kotlin Multiplatform projects targeting the **Apple platform**.

The plugin uses the embedded Swift Package Manager, so **no third-party dependency is needed**, and it's less intrusive than CocoaPods.

!!! warning "Please Be Aware"

    [Pure Swift packages can't be exported to Kotlin](./section-help/faq.md#whats-a-pure-swift-package); the plugin will help you to create a bridge to bypass this issue.

    It's a manual job, but until the Swift-import is (not currently planned) available in KMP, it's the only way.


## Features

- **Create a Swift<->Kotlin bridge**: Import your own Swift code for functionality that can't be done in Kotlin.
- **Use SPM third-Party Dependency**: Add external dependency and use it inside your bridge
- **Import Swift-compatible code to Kotlin**: Enable SPM dependencies and your own Swift code to be exposed directly in your Kotlin code ([if compatible](./section-help/faq.md#when-exporting-a-product-i-have-only-swift_typedefs-or-swift_-available-in-my-kotlin-code)).


## Support My Project ⭐️

If you find this project useful, please consider giving it a star or a coffe!

[!["Buy Me A Coffee"](https://www.buymeacoffee.com/assets/img/custom_images/orange_img.png)](https://www.buymeacoffee.com/frankois944)[![GitHub stars](https://img.shields.io/github/stars/frankois944/spm4Kmp?style=social)](https://github.com/frankois944/spm4Kmp)


## Feedback

This project greatly needs feedback, feature requests, and information about the edge case for progressing; let's [talk](https://github.com/frankois944/spm4Kmp/discussions).

## Example

A [sample](https://github.com/frankois944/spm4Kmp/tree/main/example) is available for people wanted to see the usage.
