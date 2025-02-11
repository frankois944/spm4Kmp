package io.github.frankois944.spmForKmp.tasks.apple

import io.github.frankois944.spmForKmp.*
import io.github.frankois944.spmForKmp.config.AppleCompileTarget
import io.github.frankois944.spmForKmp.definition.PackageRootDefinitionExtension
import io.github.frankois944.spmForKmp.definition.SwiftDependency
import io.github.frankois944.spmForKmp.tasks.utils.*
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider
import org.gradle.internal.extensions.stdlib.capitalized
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.tasks.CInteropProcess
import org.jetbrains.kotlin.konan.target.HostManager
import java.io.File

@Suppress("LongMethod", "LongParameterList")
internal fun Project.configAppleTargets(
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
            }.mapNotNull { AppleCompileTarget.byKonanName(it.konanTarget.name) }

    if (allTargets.isEmpty()) {
        return
    }

    val kotlinExtension =
        extensions.getByName("kotlin") as KotlinMultiplatformExtension

    val manifestTask =
        tasks.register(
            getTaskName(TASK_GENERATE_MANIFEST, extension.name),
            GenerateManifestTask::class.java,
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

    val extensionNameCapitalized = extension.name.capitalized()
    val exportedManifestDirectory =
        layout.projectDirectory
            .asFile
            .resolve("exported$extensionNameCapitalized")

    val exportedManifestTask: TaskProvider<GenerateExportableManifestTask>? =
        if (extension.packageDependencies.isNotEmpty()) {
            tasks.register(
                getTaskName(TASK_GENERATE_EXPORTABLE_PACKAGE, extension.name),
                GenerateExportableManifestTask::class.java,
            ) { taskConfig ->
                configureExportableManifestTask(
                    taskConfig,
                    extension.packageDependencies,
                    extension,
                    exportedManifestDirectory.name,
                    exportedManifestDirectory,
                )
            }
        } else {
            exportedManifestDirectory.deleteRecursively()
            null
        }

    allTargets.forEach { cinteropTarget ->

        val buildMode = getBuildMode(extension)
        val targetBuildDir = getTargetBuildDirectory(packageScratchDir, cinteropTarget, buildMode)

        val compileTask =
            tasks.register(
                getTaskName(TASK_COMPILE_PACKAGE, extension.name, cinteropTarget.name),
                CompileSwiftPackageTask::class.java,
            ) { compileTaskConfig ->
                compileTaskConfig.manifestFile.set(File(sourcePackageDir, SWIFT_PACKAGE_NAME))
                compileTaskConfig.target.set(cinteropTarget)
                compileTaskConfig.debugMode.set(extension.debug)
                compileTaskConfig.packageScratchDir.set(packageScratchDir)
                compileTaskConfig.compiledTargetDir.set(targetBuildDir)
                compileTaskConfig.sourcePackage.set(swiftSourcePackageDir)

                compileTaskConfig.osVersion.set(
                    computeOsVersion(cinteropTarget, extension),
                )

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
                GenerateCInteropDefinitionTask::class.java,
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

        if (outputFiles.isNotEmpty() && HostManager.hostIsMac) {
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
                                exportedManifestTask?.get(),
                            ),
                        ),
                )
    }
}

@Suppress("LongParameterList")
private fun configureManifestTask(
    taskConfig: GenerateManifestTask,
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

private fun configureExportableManifestTask(
    taskConfig: GenerateExportableManifestTask,
    dependencies: List<SwiftDependency>,
    extension: PackageRootDefinitionExtension,
    packageName: String,
    manifestDir: File,
) {
    taskConfig.packageDependencies.set(dependencies)
    taskConfig.packageName.set(packageName)
    taskConfig.minIos.set(extension.minIos)
    taskConfig.minTvos.set(extension.minTvos)
    taskConfig.minMacos.set(extension.minMacos)
    taskConfig.minWatchos.set(extension.minWatchos)
    taskConfig.toolsVersion.set(extension.toolsVersion)
    manifestDir.mkdirs()
    taskConfig.manifestFile.set(manifestDir.resolve(SWIFT_PACKAGE_NAME))
}

@Suppress("LongParameterList")
private fun configureGenerateCInteropDefinitionTask(
    task: GenerateCInteropDefinitionTask,
    targetBuildDir: File,
    compiledBinaryName: String,
    cinteropTarget: AppleCompileTarget,
    extension: PackageRootDefinitionExtension,
    sourcePackageDir: File,
    packageScratchDir: File,
) {
    task.compiledBinary.set(targetBuildDir.resolve(compiledBinaryName))
    task.target.set(cinteropTarget)
    task.productName.set(extension.name)
    task.packages.set(extension.packageDependencies)
    task.debugMode.set(extension.debug)
    task.osVersion.set(
        computeOsVersion(cinteropTarget, extension),
    )
    task.manifestFile.set(sourcePackageDir.resolve(SWIFT_PACKAGE_NAME))
    task.scratchDir.set(packageScratchDir)
    task.packageDependencyPrefix.set(extension.packageDependencyPrefix)
    task.compilerOpts.set(extension.compilerOpts)
    task.linkerOpts.set(extension.linkerOpts)
}

private fun computeOsVersion(
    target: AppleCompileTarget,
    extension: PackageRootDefinitionExtension,
): String =
    target.getOsVersion(
        minIos = extension.minIos,
        minWatchos = extension.minWatchos,
        minTvos = extension.minTvos,
        minMacos = extension.minMacos,
    )





