# spmForKmp Plugin

The `spmForKmp` plugin (`fr.frankois944.spmForKmp.plugin`) is a Gradle plugin designed to simplify integrating Swift Package Manager (SPM) dependencies into Kotlin Multiplatform (KMP) projects. It allows you to effortlessly configure and use Swift packages in your Kotlin projects targeting Apple platforms, such as iOS.

---

## Features

- **Support for SPM Dependencies**: Seamlessly add remote SPM dependencies to your KMP modules.
- **KMP Compatibility**: Configure Swift packages for iOS ARM64 and simulator targets.
- **Export Dependencies to Kotlin**: Enable specific SPM dependencies to be exposed directly in your Kotlin code.
- **Automatic CInterop Configuration**: Simplify the process of creating native CInterop definitions for your Swift packages.

---

## Prerequisites

Before using the `spmForKmp` plugin, ensure the following:

1. Your project is set up as a Kotlin Multiplatform (KMP) project.
2. You are using the Gradle build system.
3. You have added Apple targets (e.g., `iosArm64`, `iosSimulatorArm64`, etc.) to your KMP project.

---

## Getting Started

Follow these steps to add and configure the `spmForKmp` plugin in your project:

### 1. Apply the Plugin

Add the plugin to your `build.gradle.kts` or the appropriate Gradle module’s `plugins` block:

```kotlin
plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("fr.frankois944.spmForKmp.plugin") // Apply the spmForKmp plugin
}
```

---

### 2. Configure Kotlin Multiplatform Targets

Make sure you define your Kotlin Multiplatform targets for iOS. Here is an example configuration:

```kotlin
kotlin {
    listOf(
        iosArm64(),           // iOS ARM64 (e.g., for physical devices)
        iosSimulatorArm64()   // iOS Simulator ARM64 (e.g., for M1/M2 machines)
    ).forEach {
        it.compilations {
            val main by getting {
                cinterops.create("nativeExample") // Create a CInterop for `nativeExample`
            }
        }
    }
}
```

---

### 3. Configure Swift Package Dependencies

To use Swift Package Manager (SPM) dependencies in your project, define them in your `swiftPackageConfig` block.

```kotlin
swiftPackageConfig {
    create("nativeExample") {
        dependency(
            SwiftDependency.Package.Remote.Version(
                url = "https://github.com/krzyzanowskim/CryptoSwift.git", // Repository URL
                names = listOf("CryptoSwift"),                           // Library names
                version = "1.8.4",                                       // Package version
            )
        )
        dependency(
            SwiftDependency.Package.Remote.Version(
                url = "https://github.com/firebase/firebase-ios-sdk.git", // Repository URL
                names = listOf("FirebaseAnalytics", "FirebaseCore"),     // Libraries from the package
                packageName = "firebase-ios-sdk",                        // (Optional) Package name
                version = "11.6.0",                                      // Package version
                exportToKotlin = true                                    // Export to Kotlin for use in shared code
            )
        )
    }
}
```

---

### 4. Build Your Project

Run the following Gradle task to build your project and let the `spmForKmp` plugin handle the integration:

```bash
./gradlew build
```

After the build completes, the specified SPM dependencies will be integrated into your KMP project.

---

## Configuration Details

The `swiftPackageConfig` block enables precise configuration of Swift packages. Each package is specified using the `dependency` function with the following parameters:

| Parameter            | Description                                                                                       |
|----------------------|---------------------------------------------------------------------------------------------------|
| `url`               | The remote URL of the Swift package (Git repository).                                            |
| `names`             | The list of libraries in the package to link to your project.                                     |
| `version`           | The version of the package to use (e.g., `1.0.0`, or a valid semantic version).                   |
| `packageName`       | Optional: An explicit name for the package (useful for avoiding conflicts).                       |
| `exportToKotlin`    | Optional: When `true`, exports the package for consumption directly within Kotlin code.           |

---

## Example Use Case

Imagine you need to use the following Swift packages in your Kotlin Multiplatform project:

1. **CryptoSwift**: A library for cryptographic operations.
2. **Firebase iOS SDK**: A group of libraries for Firebase services.

Using this plugin, you easily add and configure these dependencies inside `swiftPackageConfig`. Both will be fetched via SPM, and the Firebase package will be exported to Kotlin for usage.

---

## Troubleshooting

- **SPM Dependency Errors**: If a build error occurs related to a Swift dependency, ensure the specified version, URL, and library names are correct.
- **Compatibility Issues**: Always ensure that your Apple targets (e.g., `iosArm64`, `iosSimulatorArm64`) are properly configured.

---

## License

This plugin is distributed under the MIT License. For more details, refer to the plugin repository/documentation.

---

For additional help or detailed documentation, refer to the official repository or contact the plugin maintainers. Happy coding! 🎉
