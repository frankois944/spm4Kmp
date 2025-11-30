@file:OptIn(ExperimentalStdlibApi::class)

package io.github.frankois944.spmForKmp.definition

import io.github.frankois944.spmForKmp.config.DEFAULT_MIN_IOS_VERSION
import io.github.frankois944.spmForKmp.config.DEFAULT_MIN_MAC_OS_VERSION
import io.github.frankois944.spmForKmp.config.DEFAULT_MIN_TV_OS_VERSION
import io.github.frankois944.spmForKmp.config.DEFAULT_MIN_WATCH_OS_VERSION
import io.github.frankois944.spmForKmp.config.DEFAULT_TOOL_VERSION
import io.github.frankois944.spmForKmp.definition.dependency.Dependency
import io.github.frankois944.spmForKmp.definition.dependency.DependencyConfig
import io.github.frankois944.spmForKmp.definition.exported.ExportedPackage
import io.github.frankois944.spmForKmp.definition.exported.ExportedPackageConfig
import io.github.frankois944.spmForKmp.definition.packageRegistry.auth.PackageRegistryAuth
import io.github.frankois944.spmForKmp.definition.packageSetting.BridgeSettings
import io.github.frankois944.spmForKmp.definition.packageSetting.BridgeSettingsConfig
import org.gradle.api.Project
import java.io.File
import java.net.URI
import javax.inject.Inject
import kotlin.io.path.Path
import kotlin.io.path.pathString

/**
 * Represents a definition extension for a Swift package root within a Kotlin Multiplatform project.
 *
 * This abstract class provides configuration properties and methods to define a Swift package root during
 * project setup. It allows setting up package parameters such as minimum platform versions, toolchains,
 * and handling dependencies for Swift packages.
 *
 * @constructor Initializes the package root definition extension with a name and associated project.
 * @param name The name of the package root definition.
 * @param project The Gradle project instance associated with this configuration.
 */
@Suppress("UnnecessaryAbstractClass")
public abstract class PackageRootDefinitionExtension
    @Inject
    constructor(
        public val name: String,
        project: Project,
    ) {
        internal var useExtension: Boolean = false
        internal var targetName: String? = null
        internal var internalName: String = this.name

        /**
         * Specifies the custom source path for the Swift package in the Kotlin Multiplatform project.
         *
         * By default, this path is set to the `src/swift` directory within the project's root directory.
         * This property allows defining a different directory for the Swift package source files,
         * enabling customized project structure organization.
         */
        public var customPackageSourcePath: String =
            Path(project.projectDir.path, "src", "swift").pathString

        /**
         * Specifies the minimum iOS platform version required for the Swift package integration.
         *
         * This property determines the deployment target for the iOS platform when building the Swift package
         * within the Kotlin Multiplatform project. Modifying this value adjusts the generated build configuration
         * and compatibility of the resulting package with iOS devices and emulators.
         *
         * Default value: [DEFAULT_MIN_IOS_VERSION]
         *
         * If `null`, the platform is skipped inside the Package manifest.
         */
        public var minIos: String? = DEFAULT_MIN_IOS_VERSION

        /**
         * Specifies the minimum supported macOS version for the Swift Package Manager (SPM) integration.
         *
         * This property defines the macOS version targeted by the Swift package and its dependencies.
         * Used during the generation of SPM manifests and the compilation of Swift packages to ensure compatibility
         * with the specified macOS version.
         *
         * Default value: [DEFAULT_MIN_MAC_OS_VERSION]
         *
         * If `null`, the platform is skipped inside the Package manifest.
         */
        public var minMacos: String? = DEFAULT_MIN_MAC_OS_VERSION

        /**
         * Specifies the minimum required version of tvOS for the Swift package definition.
         *
         * This property is used to configure the minimum tvOS version that the Swift package
         * dependencies and targets must support.
         *
         * Default value: [DEFAULT_MIN_TV_OS_VERSION]
         *
         * If `null`, the platform is skipped inside the Package manifest.
         */
        public var minTvos: String? = DEFAULT_MIN_TV_OS_VERSION

        /**
         * Minimum watchOS version required for the Swift package.
         *
         * This variable is used to specify the minimum version of watchOS that a Swift package targets
         * when building or running tasks involving watchOS-specific code. It ensures compatibility
         * with the defined platform version during build processes or runtime configurations.
         *
         * Default value: [DEFAULT_MIN_WATCH_OS_VERSION]
         *
         * If `null`, the platform is skipped inside the Package manifest.
         */
        public var minWatchos: String? = DEFAULT_MIN_WATCH_OS_VERSION

        /**
         * Specifies the version of Swift tools that will be used.
         * This version determines the compatibility and features available for the Swift Package Manager.
         *
         * The `toolsVersion` value impacts the structure of the `Package.swift` manifest file and
         * the behavior of the Swift package dependencies during resolution and compilation.
         *
         * Default value: [DEFAULT_TOOL_VERSION]
         */
        public var toolsVersion: String = DEFAULT_TOOL_VERSION

        /**
         * Indicates whether the Swift package is built in debug mode.
         *
         * If set to `true`, the package is being built with debug configuration. This can be useful for
         * testing or development purposes where debug symbols and additional information are required.
         *
         * Note: release build is faster
         *
         * Default value: `false`
         */
        public var debug: Boolean = false

        /**
         * Represents a prefix used for resolving conflicts or distinguishing between multiple
         * package dependencies within a Kotlin Multiplatform project.
         * This variable can be used to customize or uniquely identify package names or references when required.
         *
         * It is nullable and, when set, the prefix will be applied to all dependencies.
         */
        public var packageDependencyPrefix: String? = null

        /**
         *  Add a custom linker flag when exporting the product to kotlin
         */
        public var linkerOpts: List<String> = emptyList()

        /**
         *  Add a custom compiler flag when exporting the product to kotlin
         */
        public var compilerOpts: List<String> = emptyList()

        internal val packageDependenciesConfig: Dependency = Dependency()

        /**
         * Adds one or more Swift dependencies to the dependency list.
         *
         * @param dependencies
         * This can include local or remote dependencies in the form of
         * Swift packages or binary `xcframework` bundles.
         * It supports different dependency models such as local, versioned
         * remote, branch-based remote, or commit-based remote dependencies.
         */
        public fun dependency(dependencies: DependencyConfig.() -> Unit) {
            packageDependenciesConfig.apply(dependencies)
        }

        /**
         * Represents the file path to the shared cache directory used by the package.
         * This path is used for caching purposes to optimize dependency management,
         * reducing redundant network calls or disk operations during the build process.
         * The cache directory can store downloaded Swift package artifacts or other
         * reusable build-related data.
         *
         * If set to `null`, the default cache location will be used, determined
         * by the underlying build tool configuration or environment settings.
         */
        public var sharedCachePath: String? = null

        /**
         * Represents the file path to the shared configuration directory.
         *
         * It is optional and can be set to null if no such shared directory is required or uses the default one.
         *
         */
        public var sharedConfigPath: String? = null

        /**
         * Specifies the shared directory path for security-related resources or configurations.
         *
         * It is optional and can be set to null if no such shared directory is required or uses the default one.
         */
        public var sharedSecurityPath: String? = null

        /**
         * The path of the directory where working SPM file(s) will be written.
         *
         * Default : `{buildDirectory}/spmKmpPlugin/`
         */
        public var spmWorkingPath: String =
            project.layout.buildDirectory.asFile
                .get()
                .path

        internal var bridgeSettings: BridgeSettingsConfig = BridgeSettings()

        /**
         * Configures bridge-level settings by applying the specified configuration options.
         *
         * This method allows customization of the bridge's build settings by providing
         * a configuration block where settings can be defined for compilers (C, C++, Swift)
         * and linker options. These settings adjust the behavior of the bridge during the build process.
         *
         * @param setting A configuration block of type `PackageSettingConfig`. The block allows
         * specifying various compiler and linker settings needed for the bridge build.
         */
        public fun bridgeSettings(setting: BridgeSettingsConfig.() -> Unit) {
            bridgeSettings.apply(setting)
        }

        /**
         * The path of the Swift command line used to build the bridge
         * You can change the version of swift used for building the bridge by setting another binary
         *
         * Default: uses the command `xcrun --sdk macosx swift` to find the Swift binary
         */
        public var swiftBinPath: String? = null

        internal var exportedPackageSettings: ExportedPackage = ExportedPackage()

        /**
         * Exported package settings
         *
         * @param setting
         * @receiver
         */
        public fun exportedPackageSettings(setting: ExportedPackageConfig.() -> Unit) {
            exportedPackageSettings.apply(setting)
        }

        /**
         * A list of enums that should be generated as Kotlin enums.
         *
         * Default: emptyList()
         *
         * [configure-enums-generation](https://kotlinlang.org/docs/native-definition-file.html#configure-enums-generation)
         */
        public var strictEnums: List<String> = emptyList()

        /**
         * A list of enums that should be generated as integral values. Strict enums
         *
         * Default: emptyList()
         *
         * [configure-enums-generation](https://kotlinlang.org/docs/native-definition-file.html#configure-enums-generation)
         */
        public var nonStrictEnums: List<String> = emptyList()

        /**
         * Wraps exceptions from Objective-C code into Kotlin exceptions with the ForeignException type
         *
         * Default: null
         *
         * [handle-objective-c-exceptions](https://kotlinlang.org/docs/native-definition-file.html#handle-objective-c-exceptions)
         */
        public var foreignExceptionMode: String? = null

        /**
         * Disables the compiler check that doesn't allow calling a non-designated Objective-C initializer as a super() constructor
         *
         * Default: null
         *
         * [allow-calling-a-non-designated-initializer](https://kotlinlang.org/docs/native-definition-file.html#allow-calling-a-non-designated-initializer)
         */
        @Suppress("MaxLineLength")
        public var disableDesignatedInitializerChecks: Boolean? = null

        /**
         * Adds a custom message, for example, to help users resolve linker errors
         *
         * Default : null
         *
         * [help-resolve-linker-errors](https://kotlinlang.org/docs/native-definition-file.html#help-resolve-linker-errors)
         */
        public var userSetupHint: String? = null

        internal var packageRegistryConfigs: MutableList<PackageRegistryAuth> = mutableListOf()

        /**
         * Configures a package registry using the specified URL.
         * This method adds a new registry configuration to the list of package registry settings.
         *
         * @param url The URL of the package registry to be added.
         */
        public fun registry(url: URI) {
            packageRegistryConfigs
                .add(
                    PackageRegistryAuth(
                        url = url,
                    ),
                )
        }

        /**
         * Configures authentication settings for a package registry by associating
         * a URL with a username and password. These settings are used to authenticate
         * access to the package registry during operations involving Swift package dependencies.
         *
         * @param url The URL of the package registry.
         * @param username The username for authenticating with the registry.
         * @param password The password for authenticating with the registry.
         */
        public fun registry(
            url: URI,
            username: String,
            password: String,
        ) {
            require(url.scheme == "https") {
                "Package registry URL must use HTTPS scheme when using username/password credential"
            }
            packageRegistryConfigs
                .add(
                    PackageRegistryAuth(
                        url = url,
                        username = username,
                        password = password,
                    ),
                )
        }

        /**
         * Configures authentication settings for a package registry by associating
         * a URL with a token. These settings are used to authenticate access
         * to the package registry during operations involving Swift package dependencies.
         *
         * @param url The URL of the package registry.
         * @param token The token used for authenticating with the registry.
         */
        public fun registry(
            url: URI,
            token: String,
        ) {
            require(url.scheme == "https") {
                "Package registry URL must use HTTPS scheme when using token authentication"
            }
            packageRegistryConfigs
                .add(
                    PackageRegistryAuth(
                        url = url,
                        token = token,
                    ),
                )
        }

        /**
         * Configures authentication settings for a package registry by associating
         * a URL with a token file. These settings are used to authenticate access
         * to the package registry during operations involving Swift package dependencies.
         *
         * @param url The URL of the package registry.
         * @param tokenFile The file containing the token for authenticating with the registry.
         */
        public fun registry(
            url: URI,
            tokenFile: File,
        ) {
            require(url.scheme == "https") {
                "Package registry URL must use HTTPS scheme when using token file authentication"
            }
            packageRegistryConfigs
                .add(
                    PackageRegistryAuth(
                        url = url,
                        tokenFile = tokenFile,
                    ),
                )
        }
    }
