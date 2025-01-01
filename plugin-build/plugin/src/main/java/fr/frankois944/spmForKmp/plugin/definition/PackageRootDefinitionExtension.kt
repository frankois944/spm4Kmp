@file:OptIn(ExperimentalStdlibApi::class)

package fr.frankois944.spmForKmp.plugin.definition

import org.gradle.api.Project
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
        /**
         * Specifies the custom source path for the Swift package in the Kotlin Multiplatform project.
         *
         * By default, this path is set to the `src/swift` directory within the project's root directory.
         * This property allows defining a different directory for the Swift package source files,
         * enabling customized project structure organization.
         */
        public var customPackageSourcePath: String = Path(project.projectDir.path, "src", "swift").pathString

        /**
         * Specifies the minimum iOS platform version required for the Swift package integration.
         *
         * This property determines the deployment target for the iOS platform when building the Swift package
         * within the Kotlin Multiplatform project. Modifying this value adjusts the generated build configuration
         * and compatibility of the resulting package with iOS devices and emulators.
         *
         * Default value: `"12.0"`
         */
        public var minIos: String = "12.0"

        /**
         * Specifies the minimum supported macOS version for the Swift Package Manager (SPM) integration.
         *
         * This property defines the macOS version targeted by the Swift package and its dependencies.
         * Used during the generation of SPM manifests and the compilation of Swift packages to ensure compatibility
         * with the specified macOS version.
         * It is a required property that influences the resulting build configuration.
         *
         * Default value: "10.13".
         */
        public var minMacos: String = "10.13"

        /**
         * Specifies the minimum required version of tvOS for the Swift package definition.
         *
         * This property is used to configure the minimum tvOS version that the Swift package
         * dependencies and targets must support. It is set to a default value of "12.0" but can
         * be overridden to adapt to specific project requirements.
         */
        public var minTvos: String = "12.0"

        /**
         * Minimum watchOS version required for the Swift package.
         *
         * This variable is used to specify the minimum version of watchOS that a Swift package targets
         * when building or running tasks involving watchOS-specific code. It ensures compatibility
         * with the defined platform version during build processes or runtime configurations.
         *
         * Default value: "4.0"
         */
        public var minWatchos: String = "4.0"

        /**
         * Specifies the version of Swift tools that will be utilized.
         * This version determines the compatibility and features available for the Swift Package Manager.
         *
         * The `toolsVersion` value impacts the structure of the `Package.swift` manifest file and
         * the behavior of the Swift package dependencies during resolution and compilation.
         *
         * Default value: "5.9"
         */
        public var toolsVersion: String = "5.9"

        /**
         * Indicates whether the Swift package is built in debug mode.
         *
         * If set to `true`, the package is being built with debug configuration. This can be useful for
         * testing or development purposes where debug symbols and additional information are required.
         * Set to `false` for release builds.
         */
        public var debug: Boolean = true

        /**
         * Internal list used to store Swift package dependencies.
         *
         * This property is mutable and holds a list of [SwiftDependency] instances. It is used to accumulate and manage
         * dependencies added through the `dependency` method in the containing class.
         */
        internal val packageDependencies: MutableList<SwiftDependency> = mutableListOf()

        /**
         * Adds a Swift dependency to the package dependencies list.
         *
         * @param dependency The Swift dependency to be added.
         * This can include local or remote dependencies in the form of
         * Swift packages or binary `xcframework` bundles.
         * It supports different dependency models such as local, versioned
         * remote, branch-based remote, or commit-based remote dependencies.
         */
        public fun dependency(dependency: SwiftDependency) {
            packageDependencies.add(dependency)
        }
    }
