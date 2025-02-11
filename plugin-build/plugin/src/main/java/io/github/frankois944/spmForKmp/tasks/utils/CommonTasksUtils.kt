package io.github.frankois944.spmForKmp.tasks.utils

import io.github.frankois944.spmForKmp.PLUGIN_NAME
import io.github.frankois944.spmForKmp.config.AppleCompileTarget
import io.github.frankois944.spmForKmp.config.LinuxCompileTarget
import io.github.frankois944.spmForKmp.definition.PackageRootDefinitionExtension
import org.gradle.internal.extensions.stdlib.capitalized
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCompilation
import java.io.File

@Suppress("MaxLineLength")
internal fun getBuildMode(extension: PackageRootDefinitionExtension): String =
    if (extension.debug) "debug" else "release"

internal fun getTaskName(
    task: String,
    extension: String,
    cinteropTarget: String? = null,
) = "${PLUGIN_NAME.capitalized()}${extension.capitalized()}${task.capitalized()}${
    cinteropTarget?.capitalized().orEmpty()
}"

internal fun getCInteropTaskName(
    name: String,
    cinteropTarget: String?,
): String = "cinterop${name.capitalized()}${cinteropTarget?.capitalized().orEmpty()}"


internal fun getTargetBuildDirectory(
    packageScratchDir: File,
    cinteropTarget: AppleCompileTarget,
    buildMode: String,
): File =
    packageScratchDir
        .resolve(cinteropTarget.getPackageBuildDir())
        .resolve(buildMode)

internal fun getTargetBuildDirectory(
    packageScratchDir: File,
    cinteropTarget: LinuxCompileTarget,
    buildMode: String,
): File =
    packageScratchDir
        .resolve(buildMode)

// Extracted function to create cinterop tasks
internal fun createCInteropTask(
    mainCompilation: KotlinNativeCompilation,
    cinteropName: String,
    file: File,
) {
    mainCompilation.cinterops.create(cinteropName) { settings ->
        settings.definitionFile.set(file)
    }
}
