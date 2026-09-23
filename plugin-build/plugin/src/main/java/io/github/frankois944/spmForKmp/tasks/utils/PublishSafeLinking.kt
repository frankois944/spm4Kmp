package io.github.frankois944.spmForKmp.tasks.utils

import io.github.frankois944.spmForKmp.config.AppleCompileTarget
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

    ktTarget.binaries.configureEach { binary ->
        binary.linkTaskProvider.configure { linkTask ->
            linkTask.toolOptions.freeCompilerArgs.addAll(
                swiftRuntimePath.map { runtimePath ->
                    publishSafeLinkerArguments(buildDirPath = buildDirPath, swiftRuntimePath = runtimePath)
                },
            )
        }
    }
}

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
): List<String> =
    listOf(
        "-linker-option",
        "-F$buildDirPath",
        "-linker-option",
        "-L$swiftRuntimePath",
    )
