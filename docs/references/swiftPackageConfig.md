# SwiftPackageConfig

Configuration block applied per Apple target via `swiftPackageConfig {}`. Controls the generated `Package.swift` manifest, deployment targets, bridge compilation, cinterop settings, and dependency resolution.

```kotlin title="build.gradle.kts"
kotlin {
    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.swiftPackageConfig(cinteropName = "nativeBridge") {
            minIos = "16.0"
            debug = false
            dependency { ... }
        }
    }
}
```

---

## Quick Reference

| Property | Type | Default |
| --- | --- | --- |
| [`customPackageSourcePath`](#custompackagesourcepath) | `String` | `src/swift` |
| [`minIos`](#minios) | `String?` | `"12.0"` |
| [`minMacos`](#minmacos) | `String?` | `"10.13"` |
| [`minTvos`](#mintvos) | `String?` | `"12.0"` |
| [`minWatchos`](#minwatchos) | `String?` | `"4.0"` |
| [`toolsVersion`](#toolsversion) | `String` | `"5.9"` |
| [`debug`](#debug) | `Boolean` | `false` |
| [`packageDependencyPrefix`](#packagedependencyprefix) | `String?` | `null` |
| [`linkerOpts`](#linkeropts) | `List<String>` | `[]` |
| [`compilerOpts`](#compileropts) | `List<String>` | `[]` |
| [`swiftBinPath`](#swiftbinpath) | `String?` | `xcrun --sdk macosx swift` |
| [`toolchain`](#toolchain) | `String?` | `null` |
| [`spmWorkingPath`](#spmworkingpath) | `String` | `{buildDir}/spmKmpPlugin/` |
| [`sharedCachePath`](#sharedcachepath) | `String?` | `null` |
| [`sharedConfigPath`](#sharedconfigpath) | `String?` | `null` |
| [`sharedSecurityPath`](#sharedsecuritypath) | `String?` | `null` |
| [`newPublicationInteroperability`](#newpublicationinteroperability) | `Boolean` | `false` |

---

## Source Paths

### customPackageSourcePath

The directory containing your Swift bridge source files. The plugin copies everything in this folder into the build directory and compiles it into the bridge.

Default: `src/swift/[cinteropName]` (or `src/swift/[targetName]` when no `cinteropName` is set)

```kotlin
swiftPackageConfig(cinteropName = "nativeBridge") {
    customPackageSourcePath = "src/custom/swift"
}
```

### spmWorkingPath

The build directory where the plugin writes generated SPM files (`Package.swift`, resolved dependencies, scratch output).

Default: `{buildDirectory}/spmKmpPlugin/`

```kotlin
var spmWorkingPath: String
```

---

## Deployment Targets

Setting a platform to `null` removes it from the generated `Package.swift` manifest entirely.

### minIos

Minimum iOS deployment target.

Default: `"12.0"`

```kotlin
minIos = "16.0"
```

### minMacos

Minimum macOS deployment target.

Default: `"10.13"`

```kotlin
minMacos = "12.0"
```

### minTvos

Minimum tvOS deployment target.

Default: `"12.0"`

```kotlin
minTvos = "16.0"
```

### minWatchos

Minimum watchOS deployment target.

Default: `"4.0"`

```kotlin
minWatchos = "7.0"
```

---

## Build Configuration

### toolsVersion

The Swift tools version written into the `Package.swift` manifest header (`// swift-tools-version:`). This controls which SPM features and manifest APIs are available.

Default: `"5.9"`

```kotlin
toolsVersion = "5.9"
```

### debug

Builds the Swift package in debug configuration instead of release. Useful during development to get debug symbols, but release builds are significantly faster.

Default: `false`

```kotlin
debug = true
```

### swiftBinPath

Path to the `swift` binary used to compile the bridge. Override this to use a specific Swift version or a custom toolchain installation.

Default: resolved via `xcrun --sdk macosx swift`

```kotlin
swiftBinPath = "/path/to/custom/swift"
```

### toolchain

Swift toolchain identifier to use when building the package. See [Custom Swift Versions & Toolchains](../section-help/tips.md#support-xcode-15-and-earlier-or-another-version-of-swift) for details.

```kotlin
toolchain = "swift-DEVELOPMENT-SNAPSHOT-2024-01-01"
```

---

## Dependencies

### dependency

Declares one or more Swift package dependencies. Supports remote (version, branch, commit) and local (source or binary) packages.

```kotlin
dependency {
    remotePackageVersion(
        url = uri("https://github.com/firebase/firebase-ios-sdk.git"),
        version = "11.0.0",
        products = {
            add("FirebaseCore", exportToKotlin = true)
        },
    )
}
```

See the full [DependencyConfig reference](dependency/dependencyConfig.md) for all available options.

### packageDependencyPrefix

A prefix applied to all dependency product names when they are imported into Kotlin. Useful to avoid name collisions or to restore the `cocoapods.` prefix style when migrating from the CocoaPods plugin.

Default: `null` (no prefix — products are imported under their own name)

```kotlin
// Without prefix:  import FirebaseCore.FIRApp
// With prefix:     import myPrefix.FirebaseCore.FIRApp
packageDependencyPrefix = "myPrefix"
```

---

## Compiler & Linker Flags

These flags are passed to cinterop when exporting products to Kotlin.

### linkerOpts

Additional linker flags passed during cinterop processing.

```kotlin
linkerOpts = listOf("-lz", "-framework", "Security")
```

### compilerOpts

Additional compiler flags passed during cinterop processing.

```kotlin
compilerOpts = listOf("-DDEBUG=1")
```

---

## Bridge & Exported Package

### bridgeSettings

Configures compiler and linker settings applied when building the Swift bridge. Accepts C, C++, Swift, and linker options.

```kotlin
bridgeSettings {
    swiftSettings {
        unsafeFlags("-enable-experimental-feature", "StrictConcurrency")
    }
}
```

See the full [BridgeSettingsConfig reference](./bridgeSettingsConfig.md).

### exportedPackageSettings

Configures the local Swift package that the plugin generates for Xcode integration. Use this to expose bridge dependencies to your Xcode app target.

```kotlin
exportedPackageSettings {
    includeProduct("FirebaseCore")
}
```

See the full [ExportedPackageConfig reference](./exportedPackageConfig.md).

---

## SPM Cache & Config Paths

These map directly to the `--cache-path`, `--config-path`, and `--security-path` flags passed to the `swift package` command. Useful for sharing SPM state across modules or pointing to a network cache.

### sharedCachePath

```kotlin
sharedCachePath = "/path/to/shared/spm-cache"
```

### sharedConfigPath

```kotlin
sharedConfigPath = "/path/to/shared/spm-config"
```

### sharedSecurityPath

```kotlin
sharedSecurityPath = "/path/to/shared/spm-security"
```

---

## Package Registry

Configures Swift package registries for dependency resolution. Registries are an alternative to Git-based package sources.

See the [Package Registry usage guide](../usages/packageRegistry.md) for a full walkthrough.

### registry

=== "Unauthenticated"

    ```kotlin
    registry(url = uri("https://registry.example.com"))
    ```

=== "Username & password"

    ```kotlin
    registry(
        url = uri("https://registry.example.com"),
        username = "user",
        password = "secret",
    )
    ```

=== "Token"

    ```kotlin
    registry(
        url = uri("https://registry.example.com"),
        token = "my-token",
    )
    ```

=== "Token file"

    ```kotlin
    registry(
        url = uri("https://registry.example.com"),
        tokenFile = file("/path/to/token.txt"),
    )
    ```

---

## Cinterop Behavior

These settings map directly to [Kotlin/Native definition file](https://kotlinlang.org/docs/native-definition-file.html) directives and are applied during cinterop generation.

### strictEnums

Enums that should be generated as proper Kotlin `enum class` types instead of integer constants.

See: [configure-enums-generation](https://kotlinlang.org/docs/native-definition-file.html#configure-enums-generation)

```kotlin
strictEnums = listOf("MyObjCEnum", "AnotherEnum")
```

### nonStrictEnums

Enums that should be generated as plain integral values rather than Kotlin enums.

See: [configure-enums-generation](https://kotlinlang.org/docs/native-definition-file.html#configure-enums-generation)

```kotlin
nonStrictEnums = listOf("NSStringEncoding")
```

### foreignExceptionMode

Wraps Objective-C exceptions into Kotlin `ForeignException` instances instead of crashing.

See: [handle-objective-c-exceptions](https://kotlinlang.org/docs/native-definition-file.html#handle-objective-c-exceptions)

```kotlin
foreignExceptionMode = "objc-wrap"
```

### disableDesignatedInitializerChecks

Disables the compiler check that prevents calling a non-designated Objective-C initializer as a `super()` constructor in Kotlin.

See: [allow-calling-a-non-designated-initializer](https://kotlinlang.org/docs/native-definition-file.html#allow-calling-a-non-designated-initializer)

```kotlin
disableDesignatedInitializerChecks = true
```

### userSetupHint

A custom message appended to linker errors, useful for guiding users toward the fix.

See: [help-resolve-linker-errors](https://kotlinlang.org/docs/native-definition-file.html#help-resolve-linker-errors)

```kotlin
userSetupHint = "Add the XYZ framework to your Xcode project's linked libraries."
```

---

## Experimental

### newPublicationInteroperability

Enables the new experimental C/Objective-C interoperability mode introduced in Kotlin 2.3.20. Applies to both the bridge and exported products.

!!! warning
    Do not publish KMP libraries compiled with this mode enabled — it is still experimental and not yet stable for distribution.

See: [New interoperability mode for C or Objective-C libraries](https://kotlinlang.org/docs/whatsnew2320.html#new-interoperability-mode-for-c-or-objective-c-libraries)

```kotlin
newPublicationInteroperability = true
```
