package io.github.frankois944.spmForKmp.tasks.utils

import io.github.frankois944.spmForKmp.PLUGIN_NAME
import io.github.frankois944.spmForKmp.SPM_ARTIFACTS_DIR_NAME
import io.github.frankois944.spmForKmp.SPM_CHECKOUTS_DIR_NAME
import io.github.frankois944.spmForKmp.config.AppleCompileTarget
import io.github.frankois944.spmForKmp.definition.PackageRootDefinitionExtension
import org.gradle.internal.extensions.stdlib.capitalized
import java.io.File

internal fun getTaskName(
    task: String,
    extension: String,
    cinteropTarget: AppleCompileTarget? = null,
) = "${PLUGIN_NAME.capitalized()}Apple${extension.capitalized()}${task.capitalized()}${
    cinteropTarget?.name?.capitalized().orEmpty()
}"

internal fun getCInteropTaskName(
    name: String,
    cinteropTarget: AppleCompileTarget?,
): String = "cinterop${name.capitalized()}${cinteropTarget?.name?.capitalized().orEmpty()}"

internal fun getBuildMode(extension: PackageRootDefinitionExtension) = if (extension.debug) "debug" else "release"

internal fun computeOsVersion(
    target: AppleCompileTarget,
    extension: PackageRootDefinitionExtension,
): String? =
    target.getOsVersion(
        minIos = extension.minIos,
        minWatchos = extension.minWatchos,
        minTvos = extension.minTvos,
        minMacos = extension.minMacos,
    )

internal fun getTargetBuildDirectory(
    packageScratchDir: File,
    cinteropTarget: AppleCompileTarget,
    buildMode: String,
): File =
    packageScratchDir
        .resolve(cinteropTarget.packageBuildDirName())
        .resolve(buildMode)

/**
 * The directory where SwiftPM stores the binary dependencies (xcframeworks) of the package.
 *
 * It lives in the scratch directory, next to the per-triple build directories, and is shared
 * by every compile target of the same package.
 */
internal fun getArtifactsDirectory(packageScratchDir: File): File = packageScratchDir.resolve(SPM_ARTIFACTS_DIR_NAME)

/**
 * The directory where SwiftPM checks out the source dependencies of the package.
 *
 * Like the artifacts directory, it lives in the scratch directory and is shared by every compile
 * target: the module maps SwiftPM generates point at the umbrella headers it contains.
 */
internal fun getCheckoutsDirectory(packageScratchDir: File): File = packageScratchDir.resolve(SPM_CHECKOUTS_DIR_NAME)
