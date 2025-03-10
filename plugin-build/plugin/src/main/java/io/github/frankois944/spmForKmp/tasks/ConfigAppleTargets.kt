package io.github.frankois944.spmForKmp.tasks

import io.github.frankois944.spmForKmp.SWIFT_PACKAGE_NAME
import io.github.frankois944.spmForKmp.TASK_COMPILE_PACKAGE
import io.github.frankois944.spmForKmp.TASK_GENERATE_CINTEROP_DEF
import io.github.frankois944.spmForKmp.TASK_GENERATE_EXPORTABLE_PACKAGE
import io.github.frankois944.spmForKmp.TASK_GENERATE_MANIFEST
import io.github.frankois944.spmForKmp.config.AppleCompileTarget
import io.github.frankois944.spmForKmp.definition.PackageRootDefinitionExtension
import io.github.frankois944.spmForKmp.definition.SwiftDependency
import io.github.frankois944.spmForKmp.tasks.apple.CompileSwiftPackageTask
import io.github.frankois944.spmForKmp.tasks.apple.GenerateCInteropDefinitionTask
import io.github.frankois944.spmForKmp.tasks.apple.GenerateExportableManifestTask
import io.github.frankois944.spmForKmp.tasks.apple.GenerateManifestTask
import io.github.frankois944.spmForKmp.tasks.utils.getCInteropTaskName
import io.github.frankois944.spmForKmp.tasks.utils.getTaskName
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider
import org.gradle.internal.extensions.stdlib.capitalized
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.tasks.CInteropProcess
import org.jetbrains.kotlin.konan.target.HostManager
import java.io.File

@Suppress("LongMethod", "LongParameterList")
internal fun Project.configAppleTargets(
    taskGroup: MutableMap<AppleCompileTarget, Task>,
    cInteropTaskNamesWithDefFile: MutableMap<String, File>,
    extension: PackageRootDefinitionExtension,
    sourcePackageDir: File,
    workingDir: File,
    swiftSourcePackageDir: File?,
) {
    val allTargets =
        tasks
            .withType(CInteropProcess::class.java)
            .filter {
                it.name.startsWith("cinterop" + extension.name.capitalized())
            }.mapNotNull { AppleCompileTarget.byKonanName(it.konanTarget.name) }

    val manifestTask =
        tasks.register(
            getTaskName(TASK_GENERATE_MANIFEST, extension.name),
            GenerateManifestTask::class.java,
        ) { taskConfig ->
            configureManifestTask(
                task = taskConfig,
                extension = extension,
                manifestFile = sourcePackageDir.resolve(SWIFT_PACKAGE_NAME),
                workingDir = workingDir,
            )
        }

    val exportedManifestDirectory =
        layout.projectDirectory
            .asFile
            .resolve("exported${extension.name.capitalized()}")

    val exportedManifestTask: TaskProvider<GenerateExportableManifestTask>? =
        if (extension.packageDependencies.isNotEmpty()) {
            tasks.register(
                getTaskName(TASK_GENERATE_EXPORTABLE_PACKAGE, extension.name),
                GenerateExportableManifestTask::class.java,
            ) { taskConfig ->
                configureExportableManifestTask(
                    task = taskConfig,
                    dependencies = extension.packageDependencies,
                    extension = extension,
                    packageName = exportedManifestDirectory.name,
                    manifestDir = exportedManifestDirectory,
                )
            }
        } else {
            exportedManifestDirectory.deleteRecursively()
            null
        }

    allTargets.forEach { cinteropTarget ->

        val compileTask =
            tasks.register(
                getTaskName(TASK_COMPILE_PACKAGE, extension.name, cinteropTarget),
                CompileSwiftPackageTask::class.java,
            ) { taskConfig ->
                configureCompileTask(
                    task = taskConfig,
                    manifestFile = File(sourcePackageDir, SWIFT_PACKAGE_NAME),
                    target = cinteropTarget,
                    extension = extension,
                    workingDir = workingDir,
                    sourcePackageDir = swiftSourcePackageDir,
                )
            }

        val definitionTask =
            tasks.register(
                getTaskName(
                    TASK_GENERATE_CINTEROP_DEF,
                    extension.name,
                    cinteropTarget,
                ),
                GenerateCInteropDefinitionTask::class.java,
            ) {
                configureGenerateCInteropDefinitionTask(
                    task = it,
                    compiledBinaryName = "${extension.name}.o",
                    cinteropTarget = cinteropTarget,
                    extension = extension,
                    sourcePackageDir = sourcePackageDir,
                    workingDir = workingDir,
                )
            }

        val outputFiles = definitionTask.get().definitionFiles

        if (outputFiles.isNotEmpty() && HostManager.hostIsMac) {
            val ktTarget =
                extensions
                    .getByType(KotlinMultiplatformExtension::class.java)
                    .targets
                    .findByName(cinteropTarget.name) as KotlinNativeTarget
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

                val cinteropTaskName = getCInteropTaskName(cinteropName, cinteropTarget)
                cInteropTaskNamesWithDefFile[cinteropTaskName] = file
            }
        }

        // Explicitly create the dependency tree for the target
        taskGroup[cinteropTarget] =
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
    task: GenerateManifestTask,
    extension: PackageRootDefinitionExtension,
    manifestFile: File,
    workingDir: File,
) {
    task.apply {
        this.packageDependencies.set(extension.packageDependencies)
        this.packageName.set(extension.name)
        this.minIos.set(extension.minIos)
        this.minTvos.set(extension.minTvos)
        this.minMacos.set(extension.minMacos)
        this.minWatchos.set(extension.minWatchos)
        this.toolsVersion.set(extension.toolsVersion)
        this.packageCachePath.set(extension.packageCachePath)
        this.manifestFile.set(manifestFile)
        this.clonedSourcePackages.set(workingDir.parentFile.resolve("clonedSourcePackages"))
    }
}

private fun configureExportableManifestTask(
    task: GenerateExportableManifestTask,
    dependencies: List<SwiftDependency>,
    extension: PackageRootDefinitionExtension,
    packageName: String,
    manifestDir: File,
) {
    task.apply {
        this.packageDependencies.set(dependencies)
        this.packageName.set(packageName)
        this.minIos.set(extension.minIos)
        this.minTvos.set(extension.minTvos)
        this.minMacos.set(extension.minMacos)
        this.minWatchos.set(extension.minWatchos)
        this.toolsVersion.set(extension.toolsVersion)
        // manifestDir.mkdirs()
        this.manifestFile.set(manifestDir.resolve(SWIFT_PACKAGE_NAME))
    }
}

@Suppress("LongParameterList")
private fun configureCompileTask(
    task: CompileSwiftPackageTask,
    manifestFile: File,
    target: AppleCompileTarget,
    extension: PackageRootDefinitionExtension,
    workingDir: File,
    sourcePackageDir: File?,
) {
    task.apply {
        this.manifestFile.set(manifestFile)
        this.target.set(target)
        this.debugMode.set(extension.debug)
        this.clonedSourcePackages.set(workingDir.parentFile.resolve("clonedSourcePackages"))
        this.buildWorkingDir.set(workingDir)
        this.sourcePackage.set(sourcePackageDir)
        this.xcodeBuildArgs.set(extension.xcodeBuildArgs)
        this.packageCachePath.set(extension.packageCachePath)
    }
}

@Suppress("LongParameterList")
private fun configureGenerateCInteropDefinitionTask(
    task: GenerateCInteropDefinitionTask,
    compiledBinaryName: String,
    cinteropTarget: AppleCompileTarget,
    extension: PackageRootDefinitionExtension,
    sourcePackageDir: File,
    workingDir: File,
) {
    task.apply {
        this.compiledBinaryName.set(compiledBinaryName)
        this.target.set(cinteropTarget)
        this.productName.set(extension.name)
        this.packages.set(extension.packageDependencies)
        this.debugMode.set(extension.debug)
        this.manifestFile.set(sourcePackageDir.resolve(SWIFT_PACKAGE_NAME))
        this.buildWorkingDir.set(workingDir)
        this.packageDependencyPrefix.set(extension.packageDependencyPrefix)
        this.compilerOpts.set(extension.compilerOpts)
        this.linkerOpts.set(extension.linkerOpts)
        this.clonedSourcePackages.set(workingDir.parentFile.resolve("clonedSourcePackages"))
    }
}

private fun createCInteropTask(
    mainCompilation: KotlinNativeCompilation,
    cinteropName: String,
    file: File,
) {
    mainCompilation.cinterops.create(cinteropName) { settings ->
        settings.definitionFile.set(file)
    }
}
