@file:OptIn(ExperimentalSpmForKmpFeature::class)

package io.github.frankois944.spmForKmp.tasks

import io.github.frankois944.spmForKmp.SWIFT_PACKAGE_NAME
import io.github.frankois944.spmForKmp.TASK_COMPILE_PACKAGE
import io.github.frankois944.spmForKmp.TASK_COPY_PACKAGE_RESOURCES
import io.github.frankois944.spmForKmp.TASK_GENERATE_CINTEROP_DEF
import io.github.frankois944.spmForKmp.TASK_GENERATE_EXPORTABLE_PACKAGE
import io.github.frankois944.spmForKmp.TASK_GENERATE_MANIFEST
import io.github.frankois944.spmForKmp.config.AppleCompileTarget
import io.github.frankois944.spmForKmp.config.PackageDirectoriesConfig
import io.github.frankois944.spmForKmp.definition.PackageRootDefinitionExtension
import io.github.frankois944.spmForKmp.definition.SwiftDependency
import io.github.frankois944.spmForKmp.definition.packageSetting.BridgeSettings
import io.github.frankois944.spmForKmp.resources.getCurrentPackagesBuiltDir
import io.github.frankois944.spmForKmp.tasks.apple.CompileSwiftPackageTask
import io.github.frankois944.spmForKmp.tasks.apple.CopyPackageResourcesTask
import io.github.frankois944.spmForKmp.tasks.apple.GenerateCInteropDefinitionTask
import io.github.frankois944.spmForKmp.tasks.apple.GenerateExportableManifestTask
import io.github.frankois944.spmForKmp.tasks.apple.GenerateManifestTask
import io.github.frankois944.spmForKmp.tasks.utils.computeOsVersion
import io.github.frankois944.spmForKmp.tasks.utils.getBuildMode
import io.github.frankois944.spmForKmp.tasks.utils.getCInteropTaskName
import io.github.frankois944.spmForKmp.tasks.utils.getTargetBuildDirectory
import io.github.frankois944.spmForKmp.tasks.utils.getTaskName
import io.github.frankois944.spmForKmp.utils.ExperimentalSpmForKmpFeature
import io.github.frankois944.spmForKmp.utils.Hashing
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

@Suppress("LongMethod")
internal fun Project.configAppleTargets(
    taskGroup: MutableMap<AppleCompileTarget, Task>,
    cInteropTaskNamesWithDefFile: MutableMap<String, File>,
    swiftPackageEntry: PackageRootDefinitionExtension,
    packageDirectoriesConfig: PackageDirectoriesConfig,
) {
    val allTargets =
        tasks
            .withType(CInteropProcess::class.java)
            .filter {
                it.name.startsWith("cinterop" + swiftPackageEntry.name.capitalized())
            }.mapNotNull { AppleCompileTarget.fromKonanTarget(it.konanTarget) }

    val packageDependencies = getCurrentDependencies(swiftPackageEntry)

    val manifestTask =
        tasks.register(
            getTaskName(TASK_GENERATE_MANIFEST, swiftPackageEntry.name),
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
            .resolve("exported${swiftPackageEntry.name.capitalized()}")

    val exportedManifestTask: TaskProvider<GenerateExportableManifestTask> =
        tasks.register(
            getTaskName(TASK_GENERATE_EXPORTABLE_PACKAGE, swiftPackageEntry.name),
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

    allTargets.forEach { cinteropTarget ->
        val targetBuildDir =
            getTargetBuildDirectory(
                packageScratchDir = packageDirectoriesConfig.packageScratchDir,
                cinteropTarget = cinteropTarget,
                buildMode = buildMode,
            )

        val copyPackageResourcesTask =
            tasks.register(
                getTaskName(TASK_COPY_PACKAGE_RESOURCES, swiftPackageEntry.name, cinteropTarget),
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
                getTaskName(TASK_COMPILE_PACKAGE, swiftPackageEntry.name, cinteropTarget),
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
                    swiftPackageEntry.name,
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
            extensions
                .getByType(KotlinMultiplatformExtension::class.java)
            val ktTarget =
                extensions
                    .getByType(KotlinMultiplatformExtension::class.java)
                    .targets
                    .findByName(cinteropTarget.name) as KotlinNativeTarget
            val mainCompilation = ktTarget.compilations.getByName("main")

            outputFiles.forEachIndexed { index, file ->
                val cinteropName =
                    if (index > 0) {
                        file.nameWithoutExtension + swiftPackageEntry.name.capitalized()
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
                                    manifestTask.get(),
                                ),
                        ),
                )
    }
    taskGroup[allTargets.first()] =
        exportedManifestTask
            .get()
            .dependsOn(taskGroup[allTargets.first()])
}

@Suppress("LongParameterList")
private fun GenerateManifestTask.configureManifestTask(
    swiftPackageEntry: PackageRootDefinitionExtension,
    packageDirectoriesConfig: PackageDirectoriesConfig,
    packageDependencies: List<SwiftDependency>,
) {
    this.packageDependencies.set(packageDependencies)
    this.packageName.set(swiftPackageEntry.name)
    this.minIos.set(swiftPackageEntry.minIos)
    this.minTvos.set(swiftPackageEntry.minTvos)
    this.minMacos.set(swiftPackageEntry.minMacos)
    this.minWatchos.set(swiftPackageEntry.minWatchos)
    this.toolsVersion.set(swiftPackageEntry.toolsVersion)
    this.manifestFile.set(packageDirectoriesConfig.spmWorkingDir.resolve(SWIFT_PACKAGE_NAME))
    this.packageScratchDir.set(packageDirectoriesConfig.packageScratchDir)
    this.sharedCacheDir.set(packageDirectoriesConfig.sharedCacheDir)
    this.sharedConfigDir.set(packageDirectoriesConfig.sharedConfigDir)
    this.sharedSecurityDir.set(packageDirectoriesConfig.sharedSecurityDir)
    this.targetSettings.set(swiftPackageEntry.bridgeSettings as BridgeSettings)
    this.swiftBinPath.set(swiftPackageEntry.swiftBinPath)
}

private fun GenerateExportableManifestTask.configureExportableManifestTask(
    swiftPackageEntry: PackageRootDefinitionExtension,
    manifestDir: File,
    packageDependencies: List<SwiftDependency>,
    targetBuildDir: File,
) {
    this.packageDependencies.set(packageDependencies)
    this.packageName.set(manifestDir.name)
    this.minIos.set(swiftPackageEntry.minIos)
    this.minTvos.set(swiftPackageEntry.minTvos)
    this.minMacos.set(swiftPackageEntry.minMacos)
    this.minWatchos.set(swiftPackageEntry.minWatchos)
    this.toolsVersion.set(swiftPackageEntry.toolsVersion)
    manifestDir.mkdirs()
    this.manifestFile.set(manifestDir.resolve(SWIFT_PACKAGE_NAME))
    this.exportedPackage.set(swiftPackageEntry.exportedPackageSettings)
    this.compiledTargetDir.set(targetBuildDir)
    this.includeProduct.set(swiftPackageEntry.exportedPackageSettings.includeProduct)
}

@Suppress("LongParameterList")
private fun CompileSwiftPackageTask.configureCompileTask(
    target: AppleCompileTarget,
    swiftPackageEntry: PackageRootDefinitionExtension,
    targetBuildDir: File,
    packageDirectoriesConfig: PackageDirectoriesConfig,
) {
    val manifestFile = packageDirectoriesConfig.spmWorkingDir.resolve(SWIFT_PACKAGE_NAME)
    this.manifestFile.set(manifestFile)
    this.target.set(target)
    this.debugMode.set(swiftPackageEntry.debug)
    this.packageScratchDir.set(packageDirectoriesConfig.packageScratchDir)
    this.compiledTargetDir.set(targetBuildDir)
    this.bridgeSourceDir.set(packageDirectoriesConfig.bridgeSourceDir)
    this.osVersion.set(computeOsVersion(target, swiftPackageEntry))
    this.sharedCacheDir.set(packageDirectoriesConfig.sharedCacheDir)
    this.sharedConfigDir.set(packageDirectoriesConfig.sharedConfigDir)
    this.sharedSecurityDir.set(packageDirectoriesConfig.sharedSecurityDir)
    this.swiftBinPath.set(swiftPackageEntry.swiftBinPath)
    this.bridgeSourceBuiltDir.set(manifestFile.parentFile.resolve("Sources"))
}

private fun GenerateCInteropDefinitionTask.configureGenerateCInteropDefinitionTask(
    targetBuildDir: File,
    cinteropTarget: AppleCompileTarget,
    swiftPackageEntry: PackageRootDefinitionExtension,
    packageDirectoriesConfig: PackageDirectoriesConfig,
    packageDependencies: List<SwiftDependency>,
) {
    this.compiledBinary.set(targetBuildDir.resolve("lib${swiftPackageEntry.name}.a"))
    this.target.set(cinteropTarget)
    this.productName.set(swiftPackageEntry.name)
    this.packages.set(packageDependencies)
    this.debugMode.set(swiftPackageEntry.debug)
    this.osVersion.set(
        computeOsVersion(cinteropTarget, swiftPackageEntry),
    )
    this.manifestFile.set(packageDirectoriesConfig.spmWorkingDir.resolve(SWIFT_PACKAGE_NAME))
    this.scratchDir.set(packageDirectoriesConfig.packageScratchDir)
    this.packageDependencyPrefix.set(swiftPackageEntry.packageDependencyPrefix)
    this.compilerOpts.set(swiftPackageEntry.compilerOpts)
    this.linkerOpts.set(swiftPackageEntry.linkerOpts)
    this.swiftBinPath.set(swiftPackageEntry.swiftBinPath)
    this.currentBridgeHash.set(Hashing.hashDirectory(packageDirectoriesConfig.bridgeSourceDir))
    this.strictEnums.set(swiftPackageEntry.strictEnums)
    this.nonStrictEnums.set(swiftPackageEntry.nonStrictEnums)
    this.foreignExceptionMode.set(swiftPackageEntry.foreignExceptionMode)
    this.disableDesignatedInitializerChecks.set(swiftPackageEntry.disableDesignatedInitializerChecks)
    this.userSetupHint.set(swiftPackageEntry.userSetupHint)
}

@Suppress("LongParameterList")
private fun CopyPackageResourcesTask.configureCopyPackageResourcesTask(
    packageDirectoriesConfig: PackageDirectoriesConfig,
    buildMode: String,
    cinteropTarget: AppleCompileTarget,
) {
    val buildProductDir: String? =
        project.findProperty("io.github.frankois944.spmForKmp.BUILT_PRODUCTS_DIR") as? String
            ?: System.getenv("BUILT_PRODUCTS_DIR")
    val contentFolderPath: String? =
        project.findProperty("io.github.frankois944.spmForKmp.CONTENTS_FOLDER_PATH") as? String
            ?: System.getenv("CONTENTS_FOLDER_PATH")
    val archs: String? =
        project.findProperty("io.github.frankois944.spmForKmp.ARCHS") as? String
            ?: System.getenv("ARCHS")
    val platformName: String? =
        project.findProperty("io.github.frankois944.spmForKmp.PLATFORM_NAME") as? String
            ?: System.getenv("PLATFORM_NAME")

    logger.debug("buildProductDir $buildProductDir")
    logger.debug("contentFolderPath $contentFolderPath")
    logger.debug("archs $archs")
    logger.debug("platformName $platformName")

    @Suppress("ComplexCondition")
    if (archs.isNullOrEmpty() ||
        platformName.isNullOrEmpty() ||
        buildProductDir.isNullOrEmpty() ||
        contentFolderPath.isNullOrEmpty()
    ) {
        enabled = false
        logger.debug("Missing variable for coping the resources, skipping the task")
        return
    }

    if (cinteropTarget.sdk() != platformName) {
        logger.debug(
            "The current cinteropTarget {} is different from the xcode platformName {}",
            cinteropTarget,
            platformName,
        )
        isEnabled = false
        return
    }

    this.builtDirectory.set(
        getCurrentPackagesBuiltDir(
            packageScratchDir = packageDirectoriesConfig.packageScratchDir,
            platformName = platformName,
            archs = archs,
            buildPackageMode = buildMode,
            logger = logger,
        ),
    )
    this.codeSignIdentityName.set(System.getenv("EXPANDED_CODE_SIGN_IDENTITY_NAME"))
    this.buildProductDir.set(buildProductDir)
    this.contentFolderPath.set(contentFolderPath)
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

private fun getCurrentDependencies(swiftPackageEntry: PackageRootDefinitionExtension): List<SwiftDependency> =
    swiftPackageEntry.packageDependenciesConfig.packageDependencies
