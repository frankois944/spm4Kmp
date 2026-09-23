@file:OptIn(ExperimentalSpmForKmpFeature::class)

package io.github.frankois944.spmForKmp.tasks

import io.github.frankois944.spmForKmp.TASK_COMPILE_PACKAGE
import io.github.frankois944.spmForKmp.TASK_COPY_PACKAGE_RESOURCES
import io.github.frankois944.spmForKmp.TASK_GENERATE_CINTEROP_DEF
import io.github.frankois944.spmForKmp.TASK_GENERATE_EXPORTABLE_PACKAGE
import io.github.frankois944.spmForKmp.TASK_GENERATE_MANIFEST
import io.github.frankois944.spmForKmp.TASK_GENERATE_REGISTRY_FILE
import io.github.frankois944.spmForKmp.TASK_RESOLVE_PACKAGE
import io.github.frankois944.spmForKmp.config.AppleCompileTarget
import io.github.frankois944.spmForKmp.config.NewPublicationInteroperabilityFeature
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
import io.github.frankois944.spmForKmp.tasks.apple.resolveSwiftPackage.ResolveSwiftPackageTask
import io.github.frankois944.spmForKmp.tasks.apple.resolveSwiftPackage.configureTask
import io.github.frankois944.spmForKmp.tasks.utils.addPublishSafeLinkerOptions
import io.github.frankois944.spmForKmp.tasks.utils.computeModuleConfigs
import io.github.frankois944.spmForKmp.tasks.utils.definitionFileOf
import io.github.frankois944.spmForKmp.tasks.utils.getBuildMode
import io.github.frankois944.spmForKmp.tasks.utils.getCInteropTaskName
import io.github.frankois944.spmForKmp.tasks.utils.resolveBuildSystem
import io.github.frankois944.spmForKmp.tasks.utils.BinaryDependencies
import io.github.frankois944.spmForKmp.tasks.utils.getArtifactsDirectory
import io.github.frankois944.spmForKmp.tasks.utils.sharedResolveDirectory
import io.github.frankois944.spmForKmp.tasks.utils.getTargetBuildDirectory
import io.github.frankois944.spmForKmp.tasks.utils.getTaskName
import io.github.frankois944.spmForKmp.utils.ExperimentalSpmForKmpFeature
import io.github.frankois944.spmForKmp.utils.compareVersions
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.gradle.internal.extensions.stdlib.capitalized
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.kotlinToolingVersion
import org.jetbrains.kotlin.gradle.plugin.mpp.DefaultCInteropSettings
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.tasks.CInteropProcess
import org.jetbrains.kotlin.konan.target.HostManager
import java.io.File

@Suppress("LongMethod", "LongParameterList")
internal fun Project.configAppleTargets(
    taskGroup: MutableMap<AppleCompileTarget, TaskProvider<*>>,
    cInteropTaskNamesWithDefFile: MutableMap<String, File>,
    cInteropTaskNamesWithProducerTask: MutableMap<String, TaskProvider<*>>,
    cInteropTaskNamesWithExportTask: MutableMap<String, TaskProvider<*>>,
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
                targets = allTargets,
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

    val resolveTask: TaskProvider<ResolveSwiftPackageTask> =
        tasks.register(
            getTaskName(TASK_RESOLVE_PACKAGE, swiftPackageEntry.internalName),
            ResolveSwiftPackageTask::class.java,
        ) {
            it.configureTask(
                swiftPackageEntry = swiftPackageEntry,
                packageDirectoriesConfig = packageDirectoriesConfig,
                packageDependencies = packageDependencies,
                targets = allTargets,
            )
        }

    val buildMode = getBuildMode(swiftPackageEntry)
    allTargets.forEachIndexed { index, cinteropTarget ->
        logger.debug("SETUP {}", cinteropTarget)
        val targetBuildDir =
            getTargetBuildDirectory(
                buildSystem = resolveBuildSystem(swiftPackageEntry),
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
                    buildSystem = resolveBuildSystem(swiftPackageEntry),
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
                )
            }

        // mustRunAfter (not dependsOn) so we get correct ordering when both targets are in the
        // build graph, without forcing unrelated targets to compile in a single-target build.
        exportedManifestTask.configure {
            it.mustRunAfter(compileTask)
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

        // Predict the definition file paths from the entry configuration instead of
        // realizing the task (`definitionTask.get()`) at configuration time.
        val definitionFolder =
            packageDirectoriesConfig.spmWorkingDir
                .resolve("defFiles")
                .resolve(cinteropTarget.toString())
        val outputFiles =
            computeModuleConfigs(
                productName = swiftPackageEntry.internalName,
                compilerOpts = swiftPackageEntry.compilerOpts,
                linkerOpts = swiftPackageEntry.linkerOpts,
                packages = packageDependencies,
            ).mapIndexed { index2, moduleConfig ->
                definitionFileOf(definitionFolder, moduleConfig, index2)
            }

        if (outputFiles.isNotEmpty() && HostManager.hostIsMac) {
            val ktTarget =
                extensions
                    .getByType(KotlinMultiplatformExtension::class.java)
                    .targets
                    .findByName(cinteropTarget.name) as KotlinNativeTarget
            val mainCompilation = ktTarget.compilations.getByName("main")

            if (swiftPackageEntry.publishSafe) {
                // The definitions no longer carry the local search paths; the binaries of this
                // project still need them to link.
                addPublishSafeLinkerOptions(
                    ktTarget = ktTarget,
                    cinteropTarget = cinteropTarget,
                    targetBuildDir = targetBuildDir,
                    binaryDependencies =
                        BinaryDependencies(
                            artifactsDir =
                                getArtifactsDirectory(
                                    sharedResolveDirectory(
                                        buildSystem = resolveBuildSystem(swiftPackageEntry),
                                        packageScratchDir = packageDirectoriesConfig.packageScratchDir,
                                    ),
                                ),
                            productName = swiftPackageEntry.internalName,
                            declared = packageDependencies,
                        ),
                )
            }

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
                    val extraOpts = mutableListOf<String>()
                    if (swiftPackageEntry.newPublicationInteroperabilityFeature &&
                        compareVersions(
                            kotlinToolingVersion.toString(),
                            NewPublicationInteroperabilityFeature.minKotlinVersion(),
                        ) >= 0
                    ) {
                        extraOpts.addAll(NewPublicationInteroperabilityFeature.extraOpts())
                    }
                    createCInteropTask(
                        mainCompilation,
                        cinteropName,
                        extraOpts + swiftPackageEntry.extraOpts,
                        file
                    )
                }
                val cinteropTaskName = getCInteropTaskName(cinteropName, cinteropTarget)
                cInteropTaskNamesWithDefFile[cinteropTaskName] = file
                cInteropTaskNamesWithProducerTask[cinteropTaskName] = definitionTask
                cInteropTaskNamesWithExportTask[cinteropTaskName] = exportedManifestTask
            }
        }

        // Clean, lazy dependency wiring (no nested dependsOn, minimal .get()).
        packageRegistryTask.configure {
            it.dependsOn(manifestTask)
        }
        resolveTask.configure {
            it.dependsOn(packageRegistryTask)
        }
        compileTask.configure {
            it.dependsOn(resolveTask)
        }
        copyPackageResourcesTask.configure {
            it.dependsOn(compileTask)
        }
        definitionTask.configure {
            it.dependsOn(copyPackageResourcesTask)
        }

        // Keep a handle to the "root" for this target (as a provider, no realization)
        taskGroup[cinteropTarget] = definitionTask
    }
}

internal fun createCInteropTask(
    mainCompilation: KotlinNativeCompilation,
    cinteropName: String,
    extraOpts: List<String>,
    file: File? = null,
): DefaultCInteropSettings =
    mainCompilation.cinterops.create(cinteropName) { settings ->
        settings.extraOpts = extraOpts
        file?.let {
            settings.definitionFile.set(file)
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
