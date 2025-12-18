@file:OptIn(ExperimentalSpmForKmpFeature::class)

package io.github.frankois944.spmForKmp.tasks

import io.github.frankois944.spmForKmp.TASK_COMPILE_PACKAGE
import io.github.frankois944.spmForKmp.TASK_COPY_PACKAGE_RESOURCES
import io.github.frankois944.spmForKmp.TASK_GENERATE_CINTEROP_DEF
import io.github.frankois944.spmForKmp.TASK_GENERATE_EXPORTABLE_PACKAGE
import io.github.frankois944.spmForKmp.TASK_GENERATE_MANIFEST
import io.github.frankois944.spmForKmp.TASK_GENERATE_REGISTRY_FILE
import io.github.frankois944.spmForKmp.config.AppleCompileTarget
import io.github.frankois944.spmForKmp.config.PackageDirectoriesConfig
import io.github.frankois944.spmForKmp.definition.PackageRootDefinitionExtension
import io.github.frankois944.spmForKmp.definition.SwiftDependency
import io.github.frankois944.spmForKmp.tasks.apple.compileSwiftPackage.CompileSwiftPackageTask
import io.github.frankois944.spmForKmp.tasks.apple.compileSwiftPackage.configureTask
import io.github.frankois944.spmForKmp.tasks.apple.configRegistryPackage.ConfigRegistryPackageTask
import io.github.frankois944.spmForKmp.tasks.apple.configRegistryPackage.configureTask
import io.github.frankois944.spmForKmp.tasks.apple.copyPackageResources.CopyPackageResourcesTask
import io.github.frankois944.spmForKmp.tasks.apple.copyPackageResources.configureTask
import io.github.frankois944.spmForKmp.tasks.apple.generateCInteropDefinition.GenerateCInteropDefinitionTask
import io.github.frankois944.spmForKmp.tasks.apple.generateCInteropDefinition.configureTask
import io.github.frankois944.spmForKmp.tasks.apple.generateExportableManifest.GenerateExportableManifestTask
import io.github.frankois944.spmForKmp.tasks.apple.generateExportableManifest.configureTask
import io.github.frankois944.spmForKmp.tasks.apple.generateManifest.GenerateManifestTask
import io.github.frankois944.spmForKmp.tasks.apple.generateManifest.configureTask
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
            it.configureTask(
                swiftPackageEntry = swiftPackageEntry,
                packageDirectoriesConfig = packageDirectoriesConfig,
                packageDependencies = packageDependencies,
            )
        }

    logger.debug("NEW TASK exportedManifestTask {}", swiftPackageEntry.internalName)
    val exportedManifestTask: TaskProvider<GenerateExportableManifestTask> =
        tasks.register(
            getTaskName(TASK_GENERATE_EXPORTABLE_PACKAGE, swiftPackageEntry.internalName),
            GenerateExportableManifestTask::class.java,
        ) {
            it.configureTask(
                swiftPackageEntry = swiftPackageEntry,
                packageDirectoriesConfig = packageDirectoriesConfig,
                packageDependencies = packageDependencies,
                targets = allTargets,
            )
        }

    val packageRegistryTask: TaskProvider<ConfigRegistryPackageTask> =
        tasks.register(
            getTaskName(TASK_GENERATE_REGISTRY_FILE, swiftPackageEntry.internalName),
            ConfigRegistryPackageTask::class.java,
        ) {
            it.configureTask(
                swiftPackageEntry = swiftPackageEntry,
                packageDirectoriesConfig = packageDirectoriesConfig,
            )
        }

    val buildMode = getBuildMode(swiftPackageEntry)
    allTargets.forEachIndexed { index, cinteropTarget ->
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
                it.configureTask(
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
                it.configureTask(
                    cinteropTarget = cinteropTarget,
                    swiftPackageEntry = swiftPackageEntry,
                    packageDirectoriesConfig = packageDirectoriesConfig,
                    targetBuildDir = targetBuildDir,
                    isFirstTarget = index == 0,
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
                it.configureTask(
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

            outputFiles.forEachIndexed { cindex, file ->

                val cinteropName =
                    if (cindex > 0) {
                        if (swiftPackageEntry.useExtension) {
                            file.nameWithoutExtension
                        } else {
                            file.nameWithoutExtension + swiftPackageEntry.internalName.capitalized()
                        }
                    } else {
                        file.nameWithoutExtension.split("_").first()
                    }

                if (cindex > 0) {
                    createCInteropTask(mainCompilation, cinteropName, file)
                }

                val cinteropTaskName = getCInteropTaskName(cinteropName, cinteropTarget)
                cInteropTaskNamesWithDefFile[cinteropTaskName] = file
            }
        }

        // Clean, lazy dependency wiring (no nested dependsOn, minimal .get()).
        packageRegistryTask.configure {
            it.dependsOn(manifestTask)
        }
        compileTask.configure {
            it.dependsOn(packageRegistryTask)
        }
        copyPackageResourcesTask.configure {
            it.dependsOn(compileTask)
        }
        definitionTask.configure {
            it.dependsOn(copyPackageResourcesTask)
        }

        // Keep a handle to the "root" for this target
        taskGroup[cinteropTarget] = definitionTask.get()
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
