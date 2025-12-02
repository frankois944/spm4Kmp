@file:OptIn(ExperimentalSpmForKmpFeature::class)

package io.github.frankois944.spmForKmp.tasks

import io.github.frankois944.spmForKmp.TASK_COMPILE_PACKAGE
import io.github.frankois944.spmForKmp.TASK_COPY_PACKAGE_RESOURCES
import io.github.frankois944.spmForKmp.TASK_GENERATE_CINTEROP_DEF
import io.github.frankois944.spmForKmp.TASK_GENERATE_EXPORTABLE_PACKAGE
import io.github.frankois944.spmForKmp.TASK_GENERATE_MANIFEST
import io.github.frankois944.spmForKmp.TASK_GENERATE_REGISTRY_FILE
import io.github.frankois944.spmForKmp.TASK_RESOLVE_MANIFEST
import io.github.frankois944.spmForKmp.config.AppleCompileTarget
import io.github.frankois944.spmForKmp.config.PackageDirectoriesConfig
import io.github.frankois944.spmForKmp.definition.PackageRootDefinitionExtension
import io.github.frankois944.spmForKmp.definition.SwiftDependency
import io.github.frankois944.spmForKmp.tasks.apple.CompileSwiftPackageTask
import io.github.frankois944.spmForKmp.tasks.apple.ConfigRegistryPackageTask
import io.github.frankois944.spmForKmp.tasks.apple.CopyPackageResourcesTask
import io.github.frankois944.spmForKmp.tasks.apple.GenerateCInteropDefinitionTask
import io.github.frankois944.spmForKmp.tasks.apple.GenerateExportableManifestTask
import io.github.frankois944.spmForKmp.tasks.apple.GenerateManifestTask
import io.github.frankois944.spmForKmp.tasks.apple.ResolveManifestTask
import io.github.frankois944.spmForKmp.tasks.tasks.configureCompileTask
import io.github.frankois944.spmForKmp.tasks.tasks.configureCopyPackageResourcesTask
import io.github.frankois944.spmForKmp.tasks.tasks.configureExportableManifestTask
import io.github.frankois944.spmForKmp.tasks.tasks.configureGenerateCInteropDefinitionTask
import io.github.frankois944.spmForKmp.tasks.tasks.configureManifestTask
import io.github.frankois944.spmForKmp.tasks.tasks.configureResolveManifestTask
import io.github.frankois944.spmForKmp.tasks.utils.getBuildMode
import io.github.frankois944.spmForKmp.tasks.utils.getCInteropTaskName
import io.github.frankois944.spmForKmp.tasks.utils.getTargetBuildDirectory
import io.github.frankois944.spmForKmp.tasks.utils.getTaskName
import io.github.frankois944.spmForKmp.utils.ExperimentalSpmForKmpFeature
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider
import org.gradle.internal.extensions.stdlib.capitalized
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.DefaultCInteropSettings
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.tasks.CInteropProcess
import org.jetbrains.kotlin.konan.target.HostManager
import java.io.File

@Suppress("LongMethod")
internal fun Project.configAppleTargets(
    taskGroup: MutableMap<AppleCompileTarget, Task>,
    cInteropTaskNamesWithDefFile: MutableMap<String, File>,
    swiftPackageEntry: PackageRootDefinitionExtension,
    packageDirectoriesConfig: PackageDirectoriesConfig,
) {
    val allTargets = getAllTargets(swiftPackageEntry)
    if (allTargets.isEmpty()) {
        logger.error("No valid configuration found for {}", swiftPackageEntry.internalName)
        return
    }

    val packageDependencies = getCurrentDependencies(swiftPackageEntry)

    val manifestTask =
        tasks.register(
            getTaskName(TASK_GENERATE_MANIFEST, swiftPackageEntry.internalName),
            GenerateManifestTask::class.java,
        ) {
            it.configureManifestTask(
                swiftPackageEntry = swiftPackageEntry,
                packageDirectoriesConfig = packageDirectoriesConfig,
                packageDependencies = packageDependencies,
            )
        }

    val buildMode = getBuildMode(swiftPackageEntry)

    val exportedManifestDirectory =
        layout.projectDirectory
            .asFile
            .resolve("exported${swiftPackageEntry.internalName.capitalized()}")

    logger.debug("NEW TASK exportedManifestTask {}", swiftPackageEntry.internalName)
    val exportedManifestTask: TaskProvider<GenerateExportableManifestTask> =
        tasks.register(
            getTaskName(TASK_GENERATE_EXPORTABLE_PACKAGE, swiftPackageEntry.internalName),
            GenerateExportableManifestTask::class.java,
        ) {
            it.configureExportableManifestTask(
                swiftPackageEntry = swiftPackageEntry,
                manifestDir = exportedManifestDirectory,
                packageDependencies = packageDependencies,
                targetBuildDir =
                    getTargetBuildDirectory(
                        packageScratchDir = packageDirectoriesConfig.packageScratchDir,
                        cinteropTarget = allTargets.first(),
                        buildMode = buildMode,
                    ),
            )
        }

    val packageRegistryTask: TaskProvider<ConfigRegistryPackageTask> =
        tasks.register(
            getTaskName(TASK_GENERATE_REGISTRY_FILE, swiftPackageEntry.internalName),
            ConfigRegistryPackageTask::class.java,
        ) {
            it.workingDir.set(packageDirectoriesConfig.spmWorkingDir)
            it.swiftBinPath.set(swiftPackageEntry.swiftBinPath)
            it.registries.set(swiftPackageEntry.packageRegistryConfigs)
        }

    val resolveManifestTask =
        tasks.register(
            getTaskName(TASK_RESOLVE_MANIFEST, swiftPackageEntry.internalName),
            ResolveManifestTask::class.java,
        ) {
            it.configureResolveManifestTask(
                swiftPackageEntry = swiftPackageEntry,
                packageDirectoriesConfig = packageDirectoriesConfig,
                packageDependencies = packageDependencies,
            )
        }

    allTargets.forEach { cinteropTarget ->
        logger.debug("SETUP {}", cinteropTarget)
        val targetBuildDir =
            getTargetBuildDirectory(
                packageScratchDir = packageDirectoriesConfig.packageScratchDir,
                cinteropTarget = cinteropTarget,
                buildMode = buildMode,
            )

        val copyPackageResourcesTask =
            tasks.register(
                getTaskName(TASK_COPY_PACKAGE_RESOURCES, swiftPackageEntry.internalName, cinteropTarget),
                CopyPackageResourcesTask::class.java,
            ) {
                it.configureCopyPackageResourcesTask(
                    packageDirectoriesConfig = packageDirectoriesConfig,
                    buildMode = buildMode,
                    cinteropTarget = cinteropTarget,
                )
            }

        val compileTask =
            tasks.register(
                getTaskName(TASK_COMPILE_PACKAGE, swiftPackageEntry.internalName, cinteropTarget),
                CompileSwiftPackageTask::class.java,
            ) {
                it.configureCompileTask(
                    target = cinteropTarget,
                    swiftPackageEntry = swiftPackageEntry,
                    targetBuildDir = targetBuildDir,
                    packageDirectoriesConfig = packageDirectoriesConfig,
                )
            }

        val definitionTask =
            tasks.register(
                getTaskName(
                    TASK_GENERATE_CINTEROP_DEF,
                    swiftPackageEntry.internalName,
                    cinteropTarget,
                ),
                GenerateCInteropDefinitionTask::class.java,
            ) {
                it.configureGenerateCInteropDefinitionTask(
                    targetBuildDir = targetBuildDir,
                    cinteropTarget = cinteropTarget,
                    swiftPackageEntry = swiftPackageEntry,
                    packageDirectoriesConfig = packageDirectoriesConfig,
                    packageDependencies = packageDependencies,
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
                        if (swiftPackageEntry.useExtension) {
                            file.nameWithoutExtension
                        } else {
                            file.nameWithoutExtension + swiftPackageEntry.internalName.capitalized()
                        }
                    } else {
                        file.nameWithoutExtension.split("_").first()
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
                    copyPackageResourcesTask
                        .get()
                        .dependsOn(
                            compileTask
                                .get()
                                .dependsOn(
                                    resolveManifestTask
                                        .get()
                                        .dependsOn(
                                            packageRegistryTask
                                                .get()
                                                .dependsOn(
                                                    manifestTask.get(),
                                                ),
                                        ),
                                ),
                        ),
                )
    }
    taskGroup[allTargets.first()] =
        exportedManifestTask
            .get()
            .dependsOn(taskGroup[allTargets.first()])
}

internal fun createCInteropTask(
    mainCompilation: KotlinNativeCompilation,
    cinteropName: String,
    file: File? = null,
    packageName: String? = null,
): DefaultCInteropSettings =
    mainCompilation.cinterops.create(cinteropName) { settings ->
        file?.let {
            settings.definitionFile.set(file)
        }
        packageName?.let {
            settings.packageName = packageName
        }
    }

internal fun checkExistCInteropTask(
    mainCompilation: KotlinNativeCompilation,
    cinteropName: String,
): Boolean = mainCompilation.cinterops.findByName(cinteropName) != null

private fun getCurrentDependencies(swiftPackageEntry: PackageRootDefinitionExtension): List<SwiftDependency> =
    swiftPackageEntry.packageDependenciesConfig.packageDependencies.distinctBy { it.packageName }

private fun Project.getAllTargets(swiftPackageEntry: PackageRootDefinitionExtension): List<AppleCompileTarget> =
    tasks
        .withType(CInteropProcess::class.java)
        .filter {
            it.name.startsWith("cinterop" + swiftPackageEntry.internalName.capitalized())
        }.mapNotNull { AppleCompileTarget.fromKonanTarget(it.konanTarget) }
