package io.github.frankois944.spmForKmp.tasks.utils

import io.github.frankois944.spmForKmp.PLUGIN_NAME
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
): String =
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
        .resolve(cinteropTarget.getPackageBuildDir())
        .resolve(buildMode)
