package io.github.frankois944.spmForKmp.tasks.linux

import io.github.frankois944.spmForKmp.SWIFT_PACKAGE_NAME
import io.github.frankois944.spmForKmp.TASK_COMPILE_PACKAGE
import io.github.frankois944.spmForKmp.TASK_GENERATE_CINTEROP_DEF
import io.github.frankois944.spmForKmp.TASK_GENERATE_MANIFEST
import io.github.frankois944.spmForKmp.config.LinuxCompileTarget
import io.github.frankois944.spmForKmp.definition.PackageRootDefinitionExtension
import io.github.frankois944.spmForKmp.tasks.utils.*
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.internal.extensions.stdlib.capitalized
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.tasks.CInteropProcess
import java.io.File

@Suppress("LongMethod", "LongParameterList")
internal fun Project.configLinuxTargets(
    taskGroup: MutableMap<String, Task>,
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

    if (allTargets.isEmpty()) {
        return
    }

    val manifestTask =
        tasks.register(
            getTaskName(TASK_GENERATE_MANIFEST, extension.name),
            GenerateLinuxManifestTask::class.java,
        ) { taskConfig ->
            configureManifestTask(
                taskConfig = taskConfig,
                extension = extension,
                manifestFile = sourcePackageDir.resolve(SWIFT_PACKAGE_NAME),
                packageScratchDir = packageScratchDir,
                sharedCacheDir = sharedCacheDir,
                sharedConfigDir = sharedConfigDir,
                sharedSecurityDir = sharedSecurityDir,
            )
        }

    allTargets.forEach { cinteropTarget ->
        val buildMode = getBuildMode(extension)
        val targetBuildDir = getTargetBuildDirectory(packageScratchDir, cinteropTarget, buildMode)

        val compileTask =
            tasks.register(
                getTaskName(TASK_COMPILE_PACKAGE, extension.name, cinteropTarget.name),
                CompileLinuxSwiftPackageTask::class.java,
            ) { compileTaskConfig ->
                compileTaskConfig.manifestFile.set(File(sourcePackageDir, SWIFT_PACKAGE_NAME))
                compileTaskConfig.target.set(cinteropTarget)
                compileTaskConfig.debugMode.set(extension.debug)
                compileTaskConfig.packageScratchDir.set(packageScratchDir)
                compileTaskConfig.compiledTargetDir.set(targetBuildDir)
                compileTaskConfig.sourcePackage.set(swiftSourcePackageDir)
                compileTaskConfig.osVersion.set("unknown")
                compileTaskConfig.sharedCacheDir.set(sharedCacheDir)
                compileTaskConfig.sharedConfigDir.set(sharedConfigDir)
                compileTaskConfig.sharedSecurityDir.set(sharedSecurityDir)
            }

        val compiledBinaryName = "lib${extension.name}.a"

        val definitionTask =
            tasks.register(
                getTaskName(
                    TASK_GENERATE_CINTEROP_DEF,
                    extension.name,
                    cinteropTarget.name,
                ),
                GenerateLinuxCInteropDefinitionTask::class.java,
            ) {
                configureGenerateCInteropDefinitionTask(
                    task = it,
                    targetBuildDir = targetBuildDir,
                    compiledBinaryName = compiledBinaryName,
                    cinteropTarget = cinteropTarget,
                    extension = extension,
                    sourcePackageDir = sourcePackageDir,
                    packageScratchDir = packageScratchDir,
                )
            }

        val outputFiles = definitionTask.get().outputFiles

        if (outputFiles.isNotEmpty()) {
            val ktTarget =
                kotlinExtension.targets.findByName(cinteropTarget.name) as KotlinNativeTarget
            val mainCompilation = ktTarget.compilations.getByName("main")

            outputFiles.forEachIndexed { index, file ->
                val cinteropName =
                    if (index > 0) {
                        file.nameWithoutExtension + extension.name.capitalized()
                    } else {
                        file.nameWithoutExtension
                    }

                if (index > 0) {
                    createCInteropTask(mainCompilation, cinteropName, file)
                }

                val cinteropTaskName = getCInteropTaskName(cinteropName, cinteropTarget.name)
                cInteropTaskNamesWithDefFile[cinteropTaskName] = file
            }
        }

        // Explicitly create the dependency tree for the target
        taskGroup[cinteropTarget.name] =
            definitionTask
                .get()
                .dependsOn(
                    compileTask
                        .get()
                        .dependsOn(
                            listOfNotNull(
                                manifestTask.get(),
                            ),
                        ),
                )
    }
}

@Suppress("LongParameterList")
private fun configureManifestTask(
    taskConfig: GenerateLinuxManifestTask,
    extension: PackageRootDefinitionExtension,
    manifestFile: File,
    packageScratchDir: File,
    sharedCacheDir: File?,
    sharedConfigDir: File?,
    sharedSecurityDir: File?,
) {
    taskConfig.apply {
        packageDependencies.set(extension.packageDependencies)
        packageName.set(extension.name)
        minIos.set(extension.minIos)
        minTvos.set(extension.minTvos)
        minMacos.set(extension.minMacos)
        minWatchos.set(extension.minWatchos)
        toolsVersion.set(extension.toolsVersion)
        this.manifestFile.set(manifestFile)
        this.packageScratchDir.set(packageScratchDir)
        this.sharedCacheDir.set(sharedCacheDir)
        this.sharedConfigDir.set(sharedConfigDir)
        this.sharedSecurityDir.set(sharedSecurityDir)
    }
}


@Suppress("LongParameterList")
private fun configureGenerateCInteropDefinitionTask(
    task: GenerateLinuxCInteropDefinitionTask,
    targetBuildDir: File,
    compiledBinaryName: String,
    cinteropTarget: LinuxCompileTarget,
    extension: PackageRootDefinitionExtension,
    sourcePackageDir: File,
    packageScratchDir: File
) {
    task.compiledBinary.set(targetBuildDir.resolve(compiledBinaryName))
    task.target.set(cinteropTarget)
    task.productName.set(extension.name)
    task.packages.set(extension.packageDependencies)
    task.debugMode.set(extension.debug)
    task.osVersion.set("unknown")
    task.manifestFile.set(sourcePackageDir.resolve(SWIFT_PACKAGE_NAME))
    task.scratchDir.set(packageScratchDir)
    task.packageDependencyPrefix.set(extension.packageDependencyPrefix)
    task.compilerOpts.set(extension.compilerOpts)
    task.linkerOpts.set(extension.linkerOpts)
    task.swiftSourceDir.set(sourcePackageDir.resolve("Sources"))
}

