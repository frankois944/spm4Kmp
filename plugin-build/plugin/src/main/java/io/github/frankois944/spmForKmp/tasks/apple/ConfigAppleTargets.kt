package io.github.frankois944.spmForKmp.tasks.apple

import io.github.frankois944.spmForKmp.SWIFT_PACKAGE_NAME
import io.github.frankois944.spmForKmp.TASK_COMPILE_PACKAGE
import io.github.frankois944.spmForKmp.TASK_GENERATE_CINTEROP_DEF
import io.github.frankois944.spmForKmp.TASK_GENERATE_EXPORTABLE_PACKAGE
import io.github.frankois944.spmForKmp.TASK_GENERATE_MANIFEST
import io.github.frankois944.spmForKmp.config.AppleCompileTarget
import io.github.frankois944.spmForKmp.definition.PackageRootDefinitionExtension
import io.github.frankois944.spmForKmp.definition.SwiftDependency
import io.github.frankois944.spmForKmp.tasks.utils.computeOsVersion
import io.github.frankois944.spmForKmp.tasks.utils.getBuildMode
import io.github.frankois944.spmForKmp.tasks.utils.getCInteropTaskName
import io.github.frankois944.spmForKmp.tasks.utils.getTargetBuildDirectory
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
                getTaskName(TASK_COMPILE_PACKAGE, extension.name, cinteropTarget),
                CompileSwiftPackageTask::class.java,
            ) { taskConfig ->
                // Rename parameter for brevity
                configureCompileTask(
                    taskConfig,
                    File(sourcePackageDir, SWIFT_PACKAGE_NAME),
                    cinteropTarget,
                    extension,
                    packageScratchDir,
                    targetBuildDir,
                    swiftSourcePackageDir,
                    sharedCacheDir,
                    sharedConfigDir,
                    sharedSecurityDir,
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
                    targetBuildDir = targetBuildDir,
                    compiledBinaryName = "lib${extension.name}.a",
                    cinteropTarget = cinteropTarget,
                    extension = extension,
                    sourcePackageDir = sourcePackageDir,
                    packageScratchDir = packageScratchDir,
                )
            }

        val outputFiles = definitionTask.get().outputFiles

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
private fun configureCompileTask(
    taskConfig: CompileSwiftPackageTask,
    manifestFile: File,
    target: AppleCompileTarget,
    extension: PackageRootDefinitionExtension,
    scratchDir: File,
    buildDir: File,
    sourcePackageDir: File?,
    cacheDir: File?,
    configDir: File?,
    securityDir: File?,
) {
    taskConfig.apply {
        this.manifestFile.set(manifestFile)
        this.target.set(target)
        this.debugMode.set(extension.debug)
        this.packageScratchDir.set(scratchDir)
        this.compiledTargetDir.set(buildDir)
        this.sourcePackage.set(sourcePackageDir)
        this.osVersion.set(computeOsVersion(target, extension))
        this.sharedCacheDir.set(cacheDir)
        this.sharedConfigDir.set(configDir)
        this.sharedSecurityDir.set(securityDir)
    }
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

private fun createCInteropTask(
    mainCompilation: KotlinNativeCompilation,
    cinteropName: String,
    file: File,
) {
    mainCompilation.cinterops.create(cinteropName) { settings ->
        settings.definitionFile.set(file)
    }
}
