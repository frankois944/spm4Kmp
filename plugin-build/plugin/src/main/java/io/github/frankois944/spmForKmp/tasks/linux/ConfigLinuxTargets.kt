package io.github.frankois944.spmForKmp.tasks.linux

import io.github.frankois944.spmForKmp.config.AppleCompileTarget
import io.github.frankois944.spmForKmp.config.LinuxCompileTarget
import io.github.frankois944.spmForKmp.definition.PackageRootDefinitionExtension
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.internal.extensions.stdlib.capitalized
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.tasks.CInteropProcess
import java.io.File

@Suppress("LongMethod", "LongParameterList")
internal fun Project.configLinuxTargets(
    taskGroup: MutableMap<AppleCompileTarget, Task>,
    cInteropTaskNamesWithDefFile: MutableMap<String, File>,
    extension: PackageRootDefinitionExtension,
    sourcePackageDir: File,
    packageScratchDir: File,
    sharedCacheDir: File?,
    sharedConfigDir: File?,
    sharedSecurityDir: File?,
    swiftSourcePackageDir: File?,
) {
    val allTargets =
        tasks
            .withType(CInteropProcess::class.java)
            .filter {
                it.name.startsWith("cinterop" + extension.name.capitalized())
            }.mapNotNull { LinuxCompileTarget.byKonanName(it.konanTarget.name) }

    val kotlinExtension =
        extensions.getByName("kotlin") as KotlinMultiplatformExtension
}
