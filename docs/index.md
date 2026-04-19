# Swift Package Manager For Kotlin Multiplatform

[![Gradle Plugin Portal Version](https://img.shields.io/gradle-plugin-portal/v/io.github.frankois944.spmForKmp)](https://plugins.gradle.org/plugin/io.github.frankois944.spmForKmp)
[![build&tests](https://github.com/frankois944/spm4Kmp/actions/workflows/pre-merge.yaml/badge.svg)](https://github.com/frankois944/spm4Kmp/actions/workflows/pre-merge.yaml)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=frankois944_spm4Kmp&metric=coverage)](https://sonarcloud.io/summary/new_code?id=frankois944_spm4Kmp)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=frankois944_spm4Kmp&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=frankois944_spm4Kmp)
[![GitHub License](https://img.shields.io/github/license/frankois944/spm4kmp)](https://github.com/frankois944/spm4Kmp/blob/main/LICENSE)

---

`spmForKmp` is a modern **alternative to the [deprecated CocoaPods plugin](https://blog.cocoapods.org/CocoaPods-Specs-Repo/)** for Kotlin Multiplatform projects targeting Apple platforms.

It integrates Swift Packages and bridges Swift↔Kotlin communication using the **native Swift Package Manager** — no third-party dependencies required.

!!! warning "Pure Swift Packages"
    [Pure Swift packages cannot be exported directly to Kotlin](./section-help/faq.md#whats-a-pure-swift-package). The plugin helps you create a bridge to work around this limitation — currently the most effective approach until native Swift import is supported in KMP.

---

## Features

- **Swift-Import to Kotlin** — import your own Swift code for functionality unavailable in Kotlin
- **SPM third-party dependencies** — add external packages and use them inside your bridge
- **Export to Kotlin** — expose SPM dependencies and Swift code directly in Kotlin ([when compatible](./section-help/faq.md#when-exporting-a-product-i-have-only-swift_typedefs-or-swift_-available-in-my-kotlin-code))

---

## Getting Started

Check out the [playground](./section-help/tips.md#how-can-i-import-swift-code-into-my-kotlin-code) to see how to import Swift code into Kotlin, or browse the [sample project](https://github.com/frankois944/spm4Kmp/tree/main/example) to see the plugin in action.

---

## Feedback & Support

Feature requests, bug reports, and edge-case feedback are very welcome — [join the discussion](https://github.com/frankois944/spm4Kmp/discussions).

If you find the project useful, a star or a coffee goes a long way!

[![ko-fi](https://ko-fi.com/img/githubbutton_sm.svg)](https://ko-fi.com/A0A51MG1Y9)
[![GitHub stars](https://img.shields.io/github/stars/frankois944/spm4Kmp?style=social)](https://github.com/frankois944/spm4Kmp)
