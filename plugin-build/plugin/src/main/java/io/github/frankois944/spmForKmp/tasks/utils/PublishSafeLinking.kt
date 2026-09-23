package io.github.frankois944.spmForKmp.tasks.utils

import io.github.frankois944.spmForKmp.config.AppleCompileTarget
import io.github.frankois944.spmForKmp.definition.SwiftDependency
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import java.io.File

/**
 * When `publishSafe` is enabled, the machine specific search paths are removed from the
 * generated cinterop definitions so that they don't end up in the published klib manifests.
 *
 * They are still required to link the binaries of the project that owns the Swift package
 * (test executables, frameworks, ...), so they are added back here, directly on the link
 * tasks of the current project only.
 *
 * Everything is computed lazily: `xcrun` is only invoked when a link task actually runs,
 * which keeps the configuration cache usable.
 */
internal fun Project.addPublishSafeLinkerOptions(
    ktTarget: KotlinNativeTarget,
    cinteropTarget: AppleCompileTarget,
    targetBuildDir: File,
    binaryDependencies: BinaryDependencies,
) {
    val swiftRuntimePath =
        providers
            .exec { spec ->
                spec.commandLine("xcrun", "--sdk", "macosx", "xcode-select", "-p")
            }.standardOutput
            .asText
            .map { output ->
                swiftRuntimeLibraryPath(xcodeDevPath = output, target = cinteropTarget)
            }

    val buildDirPath = targetBuildDir.absolutePath
    val binaries = binaryDependencies.declared.filterIsInstance<SwiftDependency.Binary>()

    // `native` copies every framework into the build directory, so `-F <buildDir>` covers them
    // all. `swiftbuild` leaves a binary dependency inside its xcframework, so each slice holding
    // a framework is a search path of its own. Resolved lazily: the remote ones are only
    // extracted once the resolve task has run.
    val frameworkSearchPaths =
        providers.provider {
            binaryFrameworkSearchPaths(
                dependencies = binaries,
                artifactsDir = binaryDependencies.artifactsDir,
                productName = binaryDependencies.productName,
                target = cinteropTarget,
            )
        }

    ktTarget.binaries.configureEach { binary ->
        binary.linkTaskProvider.configure { linkTask ->
            linkTask.toolOptions.freeCompilerArgs.addAll(
                swiftRuntimePath.zip(frameworkSearchPaths) { runtimePath, extraSearchPaths ->
                    publishSafeLinkerArguments(
                        buildDirPath = buildDirPath,
                        swiftRuntimePath = runtimePath,
                        extraFrameworkPaths = extraSearchPaths,
                    )
                },
            )
        }
    }
}

/**
 * The directories holding the frameworks of the binary dependencies, for [target].
 *
 * Empty under `native`, where SwiftPM copies them into the build directory, and empty for a
 * dependency whose slice carries a plain library rather than a framework.
 */
internal fun binaryFrameworkSearchPaths(
    dependencies: List<SwiftDependency.Binary>,
    artifactsDir: File,
    productName: String,
    target: AppleCompileTarget,
): List<String> =
    dependencies
        .mapNotNull { dependency ->
            resolveBinaryModule(
                dependency = dependency,
                identities = listOf(dependency.packageName, productName.lowercase()),
                moduleName = dependency.packageName,
                artifactsDir = artifactsDir,
                target = target,
            )?.takeIf { it.isFramework }
                ?.buildDir
                ?.parentFile
                ?.absolutePath
        }.distinct()

/**
 * The Swift libraries of the Xcode toolchain for [target], from the output of `xcode-select -p`.
 */
internal fun swiftRuntimeLibraryPath(
    xcodeDevPath: String,
    target: AppleCompileTarget,
): String = "${xcodeDevPath.trim()}/Toolchains/XcodeDefault.xctoolchain/usr/lib/swift/${target.sdk()}"

/**
 * The Kotlin/Native compiler arguments giving the local search paths to the linker of a binary.
 *
 * Each path is a single argument: they are passed as is to the linker, without shell quoting.
 */
internal fun publishSafeLinkerArguments(
    buildDirPath: String,
    swiftRuntimePath: String,
    extraFrameworkPaths: List<String> = emptyList(),
): List<String> =
    buildList {
        add("-linker-option")
        add("-F$buildDirPath")
        add("-linker-option")
        add("-L$swiftRuntimePath")
        extraFrameworkPaths.forEach { path ->
            add("-linker-option")
            add("-F$path")
        }
    }
