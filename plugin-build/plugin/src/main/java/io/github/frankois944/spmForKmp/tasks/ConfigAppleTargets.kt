package io.github.frankois944.spmForKmp.tasks

import io.github.frankois944.spmForKmp.SWIFT_PACKAGE_NAME
import io.github.frankois944.spmForKmp.TASK_COMPILE_PACKAGE
import io.github.frankois944.spmForKmp.TASK_GENERATE_CINTEROP_DEF
import io.github.frankois944.spmForKmp.TASK_GENERATE_EXPORTABLE_PACKAGE
import io.github.frankois944.spmForKmp.TASK_GENERATE_MANIFEST
import io.github.frankois944.spmForKmp.config.AppleCompileTarget
import io.github.frankois944.spmForKmp.definition.PackageRootDefinitionExtension
import io.github.frankois944.spmForKmp.tasks.apple.CompileSwiftPackageTask
import io.github.frankois944.spmForKmp.tasks.apple.GenerateCInteropDefinitionTask
import io.github.frankois944.spmForKmp.tasks.apple.GenerateExportableManifestTask
import io.github.frankois944.spmForKmp.tasks.apple.GenerateManifestTask
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
    spmWorkingDir: File,
    packageScratchDir: File,
    sharedCacheDir: File?,
    sharedConfigDir: File?,
    sharedSecurityDir: File?,
    bridgeSourceDir: File,
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
        ) {
            it.configureManifestTask(
                extension = extension,
                manifestFile = spmWorkingDir.resolve(SWIFT_PACKAGE_NAME),
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
            ) {
                it.configureExportableManifestTask(
                    extension,
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
            ) {
                it.configureCompileTask(
                    File(spmWorkingDir, SWIFT_PACKAGE_NAME),
                    cinteropTarget,
                    extension,
                    packageScratchDir,
                    targetBuildDir,
                    bridgeSourceDir,
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
                it.configureGenerateCInteropDefinitionTask(
                    targetBuildDir = targetBuildDir,
                    cinteropTarget = cinteropTarget,
                    extension = extension,
                    sourcePackageDir = spmWorkingDir,
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
private fun GenerateManifestTask.configureManifestTask(
    extension: PackageRootDefinitionExtension,
    manifestFile: File,
    packageScratchDir: File,
    sharedCacheDir: File?,
    sharedConfigDir: File?,
    sharedSecurityDir: File?,
) {
    this.packageDependencies.set(extension.packageDependencies)
    this.packageName.set(extension.name)
    this.minIos.set(extension.minIos)
    this.minTvos.set(extension.minTvos)
    this.minMacos.set(extension.minMacos)
    this.minWatchos.set(extension.minWatchos)
    this.toolsVersion.set(extension.toolsVersion)
    this.manifestFile.set(manifestFile)
    this.packageScratchDir.set(packageScratchDir)
    this.sharedCacheDir.set(sharedCacheDir)
    this.sharedConfigDir.set(sharedConfigDir)
    this.sharedSecurityDir.set(sharedSecurityDir)
}

private fun GenerateExportableManifestTask.configureExportableManifestTask(
    extension: PackageRootDefinitionExtension,
    manifestDir: File,
) {
    this.packageDependencies.set(extension.packageDependencies)
    this.packageName.set(manifestDir.name)
    this.minIos.set(extension.minIos)
    this.minTvos.set(extension.minTvos)
    this.minMacos.set(extension.minMacos)
    this.minWatchos.set(extension.minWatchos)
    this.toolsVersion.set(extension.toolsVersion)
    manifestDir.mkdirs()
    this.manifestFile.set(manifestDir.resolve(SWIFT_PACKAGE_NAME))
}

@Suppress("LongParameterList")
private fun CompileSwiftPackageTask.configureCompileTask(
    manifestFile: File,
    target: AppleCompileTarget,
    extension: PackageRootDefinitionExtension,
    scratchDir: File,
    buildDir: File,
    bridgeSourceDir: File,
    cacheDir: File?,
    configDir: File?,
    securityDir: File?,
) {
    this.manifestFile.set(manifestFile)
    this.target.set(target)
    this.debugMode.set(extension.debug)
    this.packageScratchDir.set(scratchDir)
    this.compiledTargetDir.set(buildDir)
    this.bridgeSourceDir.set(bridgeSourceDir)
    this.osVersion.set(computeOsVersion(target, extension))
    this.sharedCacheDir.set(cacheDir)
    this.sharedConfigDir.set(configDir)
    this.sharedSecurityDir.set(securityDir)
}

private fun GenerateCInteropDefinitionTask.configureGenerateCInteropDefinitionTask(
    targetBuildDir: File,
    cinteropTarget: AppleCompileTarget,
    extension: PackageRootDefinitionExtension,
    sourcePackageDir: File,
    packageScratchDir: File,
) {
    this.compiledBinary.set(targetBuildDir.resolve("lib${extension.name}.a"))
    this.target.set(cinteropTarget)
    this.productName.set(extension.name)
    this.packages.set(extension.packageDependencies)
    this.debugMode.set(extension.debug)
    this.osVersion.set(
        computeOsVersion(cinteropTarget, extension),
    )
    this.manifestFile.set(sourcePackageDir.resolve(SWIFT_PACKAGE_NAME))
    this.scratchDir.set(packageScratchDir)
    this.packageDependencyPrefix.set(extension.packageDependencyPrefix)
    this.compilerOpts.set(extension.compilerOpts)
    this.linkerOpts.set(extension.linkerOpts)
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
