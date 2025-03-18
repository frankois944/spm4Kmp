# SwiftPackageConfig

## customPackageSourcePath

Specifies the custom source path for the Swift package in the Kotlin Multiplatform project.

By default, this path is set to the `src/swift` directory within the project's root directory.
This property allows defining a different directory for the Swift package source files,
enabling customized project structure organization.

```kotlin
var customPackageSourcePath: String = Path(project.projectDir.path, "src", "swift").pathString
```

## minIos

Specifies the minimum iOS platform version required for the Swift package integration.

This property determines the deployment target for the iOS platform when building the Swift package
within the Kotlin Multiplatform project. Modifying this value adjusts the generated build configuration
and compatibility of the resulting package with iOS devices and emulators.

Default value: `12.0`

```kotlin
var minIos: String = DEFAULT_MIN_IOS_VERSION
```

## minMacos

Specifies the minimum supported macOS version for the Swift Package Manager (SPM) integration.

This property defines the macOS version targeted by the Swift package and its dependencies.
Used during the generation of SPM manifests and the compilation of Swift packages to ensure compatibility
with the specified macOS version.

Default value: `10.13`

```kotlin
var minMacos: String = DEFAULT_MIN_MAC_OS_VERSION
```

## minTvos

Specifies the minimum required version of tvOS for the Swift package definition.

This property is used to configure the minimum tvOS version that the Swift package
dependencies and targets must support.
[
Default value: `12.0`

```kotlin
var minTvos: String = DEFAULT_MIN_TV_OS_VERSION
```

## minWatchos

Minimum watchOS version required for the Swift package.

This variable is used to specify the minimum version of watchOS that a Swift package targets
when building or running tasks involving watchOS-specific code. It ensures compatibility
with the defined platform version during build processes or runtime configurations.

Default value: `4.0`

```kotlin
var minWatchos: String = DEFAULT_MIN_WATCH_OS_VERSION
```

## toolsVersion

Specifies the version of Swift tools that will be utilized.
This version determines the compatibility and features available for the Swift Package Manager.

The `toolsVersion` value impacts the structure of the `Package.swift` manifest file and
the behavior of the Swift package dependencies during resolution and compilation.

Default value: `5.9`

```kotlin
var toolsVersion: String = DEFAULT_TOOL_VERSION
```

## debug

Indicates whether the Swift package is built in debug mode.

If set to `true`, the package is being built with debug configuration. This can be useful for
testing or development purposes where debug symbols and additional information are required.

Note: release build are faster (apparently...)

Default value: `false`

```kotlin
var debug: Boolean
```

## packageDependencyPrefix

Represents a prefix used for resolving conflicts or distinguishing between multiple
package dependencies within a Kotlin Multiplatform project.
This variable can be utilized to customize or uniquely identify package names or references when required.

It is nullable and, when set, the prefix will be applied to all dependencies.

```kotlin
var packageDependencyPrefix: String? = null
```

## linkerOpts

Add custom linker flag when exporting the product to kotlin, used by cinterop

```kotlin
var linkerOpts: List<String> = emptyList()
```

## compilerOpts

Add custom compiler flag when exporting the product to kotlin, used by cinterop

```kotlin
var compilerOpts: List<String> = emptyList()
```

## dependency

Adds one or more Swift dependencies to the dependencies list.

- **[dependencies](dependency/dependencyConfig.md)** A configuration block of type `DependencyConfig`.

The block allows specifying various compiler and linker settings needed for the package build.

This can include local or remote dependencies in the form of
Swift packages or binary `xcframework` bundles.

It supports different dependency models such as local, versioned
remote, branch-based remote, or commit-based remote dependencies.

```kotlin
fun dependency(dependencies: DependencyConfig.() -> Unit)
```

## dependency (Deprecated)

!!! warning "Please Be Aware"

    Will be removed on version 1.0.0 and replaced by [DependencyConfig](swiftPackageConfig.md#dependency)

Adds one or more Swift dependencies to the dependencies list.

- **[dependency](dependency/swiftDependency.md)** A variable number of `SwiftDependency` instances to be added.

This can include local or remote dependencies in the form of
Swift packages or binary `xcframework` bundles.

It supports different dependency models such as local, versioned
remote, branch-based remote, or commit-based remote dependencies.

```kotlin
fun dependency(vararg dependency: SwiftDependency)
```

## sharedCachePath

Specify the shared cache directory path

```kotlin
var sharedCachePath: String? = null
```

## sharedConfigPath

Specify the shared configuration directory path

```kotlin
var sharedConfigPath: String? = null
```

## sharedSecurityPath

Specify the shared security directory path

```kotlin
var sharedSecurityPath: String? = null
```

## spmWorkingPath

The path of the directory where working SPM file(s) will be written.

Default : `{buildDirectory}/spmKmpPlugin/`

```kotlin
var spmWorkingPath: String
```

## targetSettings

Configures the bridge settings by applying the specified configuration options.

This method allows customization of the bridge's build settings by providing
a configuration block where settings can be defined for compilers (C, C++, Swift)
and linker options. These settings adjust the behavior of the bridge during the build process.

- **[setting](./bridgeSettingsConfig.md)** A configuration block of type `PackageSettingConfig`.

The block allows specifying various compiler and linker settings needed for the package build.

```kotlin
fun bridgeSettings(setting: BridgeSettingsConfig.() -> Unit)
```
