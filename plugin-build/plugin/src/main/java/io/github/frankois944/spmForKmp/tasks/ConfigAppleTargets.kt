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
import io.github.frankois944.spmForKmp.definition.dependency.Dependency
import io.github.frankois944.spmForKmp.definition.exported.ExportedPackage
import io.github.frankois944.spmForKmp.definition.packageSetting.BridgeSettings
import io.github.frankois944.spmForKmp.operations.isDynamicLibrary
import io.github.frankois944.spmForKmp.tasks.apple.CompileSwiftPackageTask
import io.github.frankois944.spmForKmp.tasks.apple.CopyPackageResourcesTask
import io.github.frankois944.spmForKmp.tasks.apple.GenerateCInteropDefinitionTask
import io.github.frankois944.spmForKmp.tasks.apple.GenerateExportableManifestTask
import io.github.frankois944.spmForKmp.tasks.apple.GenerateManifestTask
import io.github.frankois944.spmForKmp.tasks.utils.computeOsVersion
import io.github.frankois944.spmForKmp.tasks.utils.getBuildMode
import io.github.frankois944.spmForKmp.tasks.utils.getCInteropTaskName
import io.github.frankois944.spmForKmp.tasks.utils.getCopyablePackageResourceName
import io.github.frankois944.spmForKmp.tasks.utils.getTargetBuildDirectory
import io.github.frankois944.spmForKmp.tasks.utils.getTaskName
import io.github.frankois944.spmForKmp.utils.Hashing
import io.github.frankois944.spmForKmp.utils.getPlistValue
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider
import org.gradle.internal.extensions.core.serviceOf
import org.gradle.internal.extensions.stdlib.capitalized
import org.gradle.process.ExecOperations
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.tasks.CInteropProcess
import org.jetbrains.kotlin.konan.target.HostManager
import java.io.File
import java.io.FileFilter
import java.io.FilenameFilter

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

    val exportedManifestDirectory =
        layout.projectDirectory
            .asFile
            .resolve("exported${swiftPackageEntry.name.capitalized()}")

    val exportedManifestTask: TaskProvider<GenerateExportableManifestTask>? =
        if (packageDependencies.isNotEmpty()) {
            tasks.register(
                getTaskName(TASK_GENERATE_EXPORTABLE_PACKAGE, swiftPackageEntry.name),
                GenerateExportableManifestTask::class.java,
            ) {
                it.configureExportableManifestTask(
                    swiftPackageEntry = swiftPackageEntry,
                    manifestDir = exportedManifestDirectory,
                    packageDependencies = packageDependencies,
                )
            }
        } else {
            exportedManifestDirectory.deleteRecursively()
            null
        }

    val buildMode = getBuildMode(swiftPackageEntry)

    val copyPackageResourcesTask = tasks.register(
        getTaskName(TASK_COPY_PACKAGE_RESOURCES, swiftPackageEntry.name),
        CopyPackageResourcesTask::class.java
    ) {
        it.configureCopyPackageResourcesTask(
            swiftPackageEntry = swiftPackageEntry,
            packageDirectoriesConfig = packageDirectoriesConfig,
            buildMode = buildMode
        )
    }

    allTargets.forEach { cinteropTarget ->
        val targetBuildDir =
            getTargetBuildDirectory(
                packageScratchDir = packageDirectoriesConfig.packageScratchDir,
                cinteropTarget = cinteropTarget,
                buildMode = buildMode,
            )

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
            copyPackageResourcesTask
                .get()
                .dependsOn(
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
                )
    }

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
    this.exportedPackage.set(swiftPackageEntry.exportedPackageSettings as ExportedPackage)
}

@Suppress("LongParameterList")
private fun CompileSwiftPackageTask.configureCompileTask(
    target: AppleCompileTarget,
    swiftPackageEntry: PackageRootDefinitionExtension,
    targetBuildDir: File,
    packageDirectoriesConfig: PackageDirectoriesConfig,
) {
    this.manifestFile.set(packageDirectoriesConfig.spmWorkingDir.resolve(SWIFT_PACKAGE_NAME))
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

private fun getCurrentDependencies(swiftPackageEntry: PackageRootDefinitionExtension): List<SwiftDependency> {
    val newDependency = (swiftPackageEntry.packageDependenciesConfig as Dependency).packageDependencies.toList()
    val oldDependency = swiftPackageEntry.packageDependencies.toList()
    return newDependency.ifEmpty { oldDependency }
}

@Suppress("LongParameterList")
private fun CopyPackageResourcesTask.configureCopyPackageResourcesTask(
    swiftPackageEntry: PackageRootDefinitionExtension,
    packageDirectoriesConfig: PackageDirectoriesConfig,
    buildMode: String
) {
    val configuration: String? = project.findProperty("CONFIGURATION") as? String ?: System.getenv("CONFIGURATION")
    val buildProductDir: String? =
        project.findProperty("BUILT_PRODUCTS_DIR") as? String ?: System.getenv("BUILT_PRODUCTS_DIR")
    val contentFolderPath: String? =
        project.findProperty("CONTENTS_FOLDER_PATH") as? String ?: System.getenv("CONTENTS_FOLDER_PATH")
    val archs: String? = project.findProperty("ARCHS") as? String ?: System.getenv("ARCHS")
    val platformName: String? = project.findProperty("PLATFORM_NAME") as? String ?: System.getenv("PLATFORM_NAME")
    val baseDir: File = project.layout.projectDirectory.asFile
    val execOp = project.serviceOf<ExecOperations>()

    logger.warn("configuration $configuration")
    logger.warn("buildProductDir $buildProductDir")
    logger.warn("contentFolderPath $contentFolderPath")
    logger.warn("archs $archs")
    logger.warn("platformName $platformName")

    if (platformName == null || archs == null || contentFolderPath == null || buildProductDir == null || configuration == null) {
        enabled = false
        return
    }

    val copyableResource = swiftPackageEntry.packageDependencies.getCopyablePackageResourceName()

    if (copyableResource.isEmpty()) {
        logger.warn("No copyable resources found")
        enabled = false
        return
    } else {
        logger.warn("Found copyable resources $copyableResource")
    }


    val inputBaseDirectory = CopyPackageResourcesTask.getCurrentPackagesBuiltPath(
        packageDirectoriesConfig.packageScratchDir,
        platformName = platformName,
        archs = archs,
        buildPackageMode = buildMode,
        logger = logger,
    )

    val copyableScratchFiles = inputBaseDirectory.toFile()
        .listFiles { _, name ->
            copyableResource.contains(name.lowercase())
        }

    logger.warn("Found copyable products $copyableScratchFiles")

    val bundles = buildList {
        // get all bundle folders from build directory
        copyableScratchFiles?.filter {
            it.extension == "bundle"
        }?.let {
            logger.warn("Found bundles $it")
            addAll(it.map { f -> f.relativeTo(baseDir) })
        }
        // get all bundles from frameworks
        copyableScratchFiles?.filter {
            it.extension == "framework"
        }?.forEach { framework ->
            framework.listFiles(FileFilter {
                it.extension == "bundle"
            })?.let {
                logger.warn("Found framework bundles $it")
                addAll(it.map { f -> f.relativeTo(baseDir) })
            }
        }
    }


    val frameworks = buildList {
        copyableScratchFiles?.filter {
            it.extension == "framework"
        }?.forEach { framework ->
            val plist = framework.resolve("Info.plist")
            logger.warn("Looking inside the Info.plist $plist")
            val libraryName = getPlistValue(plist, "Executable file")
            logger.warn("Found libraryName $libraryName")
            if (libraryName.isNullOrEmpty()) {
                logger.error("Cant retrieve executable name from $framework")
            } else {
                val libraryFile = framework.resolve(libraryName)
                // A static library can't contain raw resource files but only bundles.
                // A dynamic library and his resources must be copied inside the Apple app.
                if (execOp.isDynamicLibrary(libraryFile, logger)) {
                    framework.listFiles()?.forEach {
                        if (!it.isDirectory) {
                            add(it.relativeTo(baseDir))
                        } else if (!listOf("Modules", "Headers", "_CodeSignature").contains(it.name)) {
                            add(it.relativeTo(baseDir))
                        }
                    }
                } else {
                    logger.warn("Ignore $libraryFile because is a static library")
                }
            }
        }
    }

    // Output Directory
    val destinationDir = File("${buildProductDir}/${contentFolderPath}")
    this.outputBundleDirectory.set(destinationDir.relativeTo(baseDir))
    this.outputFrameworkDirectory.set(destinationDir.resolve("Framework").relativeTo(baseDir))
    this.inputBundles.set(bundles)
    this.inputFrameworks.set(frameworks)
}
