# SwiftPackageConfig

### customPackageSourcePath

``` kotlin
/**
* Specifies the custom source path for the Swift package in the Kotlin Multiplatform project.
*
* By default, this path is set to the `src/swift` directory within the project's root directory.
* This property allows defining a different directory for the Swift package source files,
* enabling customized project structure organization.
*/
public var customPackageSourcePath: String = Path(project.projectDir.path, "src", "swift").pathString
```

### minIos

``` kotlin
/**
 * Specifies the minimum iOS platform version required for the Swift package integration.
 *
 * This property determines the deployment target for the iOS platform when building the Swift package
 * within the Kotlin Multiplatform project. Modifying this value adjusts the generated build configuration
 * and compatibility of the resulting package with iOS devices and emulators.
 *
 * Default value: [DEFAULT_MIN_IOS_VERSION]
 */
public var minIos: String = DEFAULT_MIN_IOS_VERSION
```

### minMacos

``` kotlin
/**
 * Specifies the minimum supported macOS version for the Swift Package Manager (SPM) integration.
 *
 * This property defines the macOS version targeted by the Swift package and its dependencies.
 * Used during the generation of SPM manifests and the compilation of Swift packages to ensure compatibility
 * with the specified macOS version.
 *
 * Default value: [DEFAULT_MIN_MAC_OS_VERSION]
 */
public var minMacos: String = DEFAULT_MIN_MAC_OS_VERSION
```

### minTvos

``` kotlin
/**
 * Specifies the minimum required version of tvOS for the Swift package definition.
 *
 * This property is used to configure the minimum tvOS version that the Swift package
 * dependencies and targets must support.
 *
 * Default value: [DEFAULT_MIN_TV_OS_VERSION]
 */
public var minTvos: String = DEFAULT_MIN_TV_OS_VERSION
```

### minWatchos

``` kotlin
/**
 * Minimum watchOS version required for the Swift package.
 *
 * This variable is used to specify the minimum version of watchOS that a Swift package targets
 * when building or running tasks involving watchOS-specific code. It ensures compatibility
 * with the defined platform version during build processes or runtime configurations.
 *
 * Default value: [DEFAULT_MIN_WATCH_OS_VERSION]
 */
public var minWatchos: String = DEFAULT_MIN_WATCH_OS_VERSION
```

### toolsVersion

``` kotlin
/**
 * Specifies the version of Swift tools that will be utilized.
 * This version determines the compatibility and features available for the Swift Package Manager.
 *
 * The `toolsVersion` value impacts the structure of the `Package.swift` manifest file and
 * the behavior of the Swift package dependencies during resolution and compilation.
 *
 * Default value: [DEFAULT_TOOL_VERSION]
 */
public var toolsVersion: String = DEFAULT_TOOL_VERSION
```

### debug

``` kotlin
/**
 * Indicates whether the Swift package is built in debug mode.
 *
 * If set to `true`, the package is being built with debug configuration. This can be useful for
 * testing or development purposes where debug symbols and additional information are required.
 *
 * Note: release build are faster
 *
 * Default value: `false`
 */
public var debug: Boolean = false
```

### packageDependencyPrefix

``` kotlin
/**
 * Represents a prefix used for resolving conflicts or distinguishing between multiple
 * package dependencies within a Kotlin Multiplatform project.
 * This variable can be utilized to customize or uniquely identify package names or references when required.
 *
 * It is nullable and, when set, the prefix will be applied to all dependencies.
 */
public var packageDependencyPrefix: String? = null
```

### linkerOpts

``` kotlin
/**
 *  Add custom linker flag when exporting the product to kotlin
 */
public var linkerOpts: List<String> = emptyList()
```

### compilerOpts

``` kotlin
/**
 *  Add custom compiler flag when exporting the product to kotlin
 */
public var compilerOpts: List<String> = emptyList()
```

### dependency

``` kotlin
/**
 * Adds one or more Swift dependencies to the dependencies list.
 *
 * @param dependency A variable number of `SwiftDependency` instances to be added.
 * This can include local or remote dependencies in the form of
 * Swift packages or binary `xcframework` bundles.
 * It supports different dependency models such as local, versioned
 * remote, branch-based remote, or commit-based remote dependencies.
 */
public fun dependency(vararg dependency: SwiftDependency)
```

### sharedCachePath

``` kotlin
/**
 * Represents the file path to the shared cache directory used by the package.
 * This path is utilized for caching purposes to optimize dependency management,
 * reducing redundant network calls or disk operations during the build process.
 * The cache directory can store downloaded Swift package artifacts or other
 * reusable build-related data.
 *
 * If set to `null`, the default cache location will be used, determined
 * by the underlying build tool configuration or environment settings.
 */
public var sharedCachePath: String? = null
```

### sharedConfigPath

``` kotlin
/**
 * Represents the file path to the shared configuration directory.
 *
 * It is optional and can be set to null if no such shared directory is required or use the default one.
 *
 */
public var sharedConfigPath: String? = null
```

### sharedSecurityPath

``` kotlin
/**
 * Specifies the shared directory path for security-related resources or configurations.
 *
 * It is optional and can be set to null if no such shared directory is required or use the default one.
 */
public var sharedSecurityPath: String? = null
```

### spmWorkingPath

``` kotlin
/**
 * The path of the directory where working SPM file(s) will be written.
 *
 * Default : `{buildDirectory}/spmKmpPlugin/`
 */
public var spmWorkingPath: String
```
