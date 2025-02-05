# Swift Package Manager for Kotlin Multiplatform

[![Gradle Plugin Portal Version](https://img.shields.io/gradle-plugin-portal/v/io.github.frankois944.spmForKmp)](https://plugins.gradle.org/plugin/io.github.frankois944.spmForKmp)
[![build&tests](https://github.com/frankois944/spm4Kmp/actions/workflows/pre-merge.yaml/badge.svg)](https://github.com/frankois944/spm4Kmp/actions/workflows/pre-merge.yaml)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=frankois944_spm4Kmp&metric=coverage)](https://sonarcloud.io/summary/new_code?id=frankois944_spm4Kmp)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=frankois944_spm4Kmp&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=frankois944_spm4Kmp)
[![GitHub License](https://img.shields.io/github/license/frankois944/spm4kmp)](https://github.com/frankois944/spm4Kmp/blob/main/LICENSE)

The Swift Package Manager for Kotlin Multiplatform Plugin, aka `spmForKmp` Gradle Plugin, is a tool designed to simplify integrating Swift Package Manager (SPM) dependencies into Kotlin Multiplatform (KMP) projects and calling the native API.

It allows you to (almost) effortlessly configure and use Swift packages in your Kotlin projects targeting Apple platforms, such as iOS.

!!! warning "Please Be Aware"

    [Pure Swift packages can't be exported to Kotlin](https://kotlinlang.org/docs/native-objc-interop.html#importing-swift-objective-c-libraries-to-kotlin); creating a bridge with this plugin is a solution to bypass this issue.

## Features

- **Create a Swift<->Kotlin bridge**: Use your own Swift code for functionality that can't be done in Kotlin.
- **Use SPM third-Party Dependency**: Add external dependency in your bridge
- **Import Swift-compatible code to Kotlin**: Enable SPM dependencies and your own Swift code to be exposed directly in your Kotlin code (if compatible).
## Feedback

This project greatly needs feedback, feature requests, and information about the edge case for progressing; let's [talk](https://github.com/frankois944/spm4Kmp/discussions).

## Example

A [sample](https://github.com/frankois944/spm4Kmp/tree/main/example) is available for people wanted to see the usage.
