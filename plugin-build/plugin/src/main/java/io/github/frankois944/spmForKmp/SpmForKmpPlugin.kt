@file:OptIn(ExperimentalStdlibApi::class)

package io.github.frankois944.spmForKmp

import io.github.frankois944.spmForKmp.config.CompileTarget
import io.github.frankois944.spmForKmp.definition.PackageRootDefinitionExtension
import io.github.frankois944.spmForKmp.definition.SwiftDependency
import io.github.frankois944.spmForKmp.tasks.CompileSwiftPackageTask
import io.github.frankois944.spmForKmp.tasks.GenerateCInteropDefinitionTask
import io.github.frankois944.spmForKmp.tasks.GenerateExportableManifestTask
import io.github.frankois944.spmForKmp.tasks.GenerateManifestTask
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.reflect.TypeOf
import org.gradle.api.tasks.TaskProvider
import org.gradle.internal.extensions.stdlib.capitalized
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.tasks.CInteropProcess
import org.jetbrains.kotlin.konan.target.HostManager
import java.io.File
import kotlin.reflect.javaType
import kotlin.reflect.typeOf

internal const val PLUGIN_NAME: String = "swiftPackageConfig"
internal const val TASK_GENERATE_MANIFEST: String = "generateSwiftPackage"
internal const val TASK_COMPILE_PACKAGE: String = "compileSwiftPackage"
internal const val TASK_GENERATE_CINTEROP_DEF: String = "generateCInteropDefinition"
internal const val TASK_GENERATE_EXPORTABLE_PACKAGE: String = "generateExportableSwiftPackage"
private const val SWIFT_PACKAGE_NAME = "Package.swift"

@Suppress("UnnecessaryAbstractClass", "TooManyFunctions")
public abstract class SpmForKmpPlugin : Plugin<Project> {
    @Suppress("LongMethod")
    override fun apply(target: Project): Unit =
        with(target) {
            val swiftPackageEntries: NamedDomainObjectContainer<out PackageRootDefinitionExtension> =
                objects.domainObjectContainer(PackageRootDefinitionExtension::class.java) { name ->
                    objects.newInstance(PackageRootDefinitionExtension::class.java, name)
                }

            val type =
                TypeOf.typeOf<NamedDomainObjectContainer<out PackageRootDefinitionExtension>>(
                    typeOf<NamedDomainObjectContainer<PackageRootDefinitionExtension>>().javaType,
                )

            extensions.add(type, PLUGIN_NAME, swiftPackageEntries)

            val kotlinExtension =
                extensions.getByName("kotlin") as KotlinMultiplatformExtension

            afterEvaluate {
                // Contains the group of task (with their dependency) by target
                val taskGroup = mutableMapOf<CompileTarget, Task>()
                // Contains the cinterop .def file linked with the task name
                val cInteropTaskNamesWithDefFile = mutableMapOf<String, File>()
                swiftPackageEntries.forEach { extension ->

                    val sourcePackageDir =
                        resolveAndCreateDir(
                            layout.buildDirectory.asFile.get(),
                            "spmKmpPlugin/${extension.name}",
                        )

                    val packageScratchDir = resolveAndCreateDir(sourcePackageDir, "scratch")
                    val sharedCacheDir: File? = extension.sharedCachePath?.let { resolveAndCreateDir(File(it)) }
                    val sharedConfigDir: File? = extension.sharedConfigPath?.let { resolveAndCreateDir(File(it)) }
                    val sharedSecurityDir: File? = extension.sharedSecurityPath?.let { resolveAndCreateDir(File(it)) }
                    val swiftSourcePackageDir =
                        resolveAndCreateDir(
                            File(extension.customPackageSourcePath),
                            extension.name,
                        )

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

                    val allTargets =
                        tasks
                            .withType(CInteropProcess::class.java)
                            .filter {
                                it.name.startsWith("cinterop" + extension.name.capitalized())
                            }.mapNotNull { CompileTarget.byKonanName(it.konanTarget.name) }

                    allTargets.forEach { cinteropTarget ->

                        val buildMode = getBuildMode(extension)
                        val targetBuildDir = getTargetBuildDirectory(packageScratchDir, cinteropTarget, buildMode)

                        val compileTask =
                            tasks.register(
                                getTaskName(TASK_COMPILE_PACKAGE, extension.name, cinteropTarget),
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

                        val compiledBinaryName =
                            if (extension.isStatic) {
                                "lib${extension.name}.a"
                            } else {
                                "lib${extension.name}.dylib"
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
                // link the main definition File
                tasks.withType(CInteropProcess::class.java).configureEach { cinterop ->
                    if (HostManager.hostIsMac) {
                        val cinteropTarget =
                            CompileTarget.byKonanName(cinterop.konanTarget.name)
                                ?: return@configureEach
                        // The cinterop task needs to run the requirement tasks before getting the .def file
                        cinterop.dependsOn(taskGroup[cinteropTarget])
                        cinterop.mustRunAfter(taskGroup[cinteropTarget])
                        val definitionFile = cInteropTaskNamesWithDefFile[cinterop.name]
                        cinterop.settings.definitionFile.set(definitionFile)
                    }
                }
            }
        }

    private fun getTaskName(
        task: String,
        extension: String,
        cinteropTarget: CompileTarget? = null,
    ) = "${PLUGIN_NAME.capitalized()}${extension.capitalized()}${task.capitalized()}${
        cinteropTarget?.name?.capitalized().orEmpty()
    }"

    private fun getCInteropTaskName(
        name: String,
        cinteropTarget: CompileTarget?,
    ): String = "cinterop${name.capitalized()}${cinteropTarget?.name?.capitalized().orEmpty()}"

    private fun Project.resolvePath(destination: File): File =
        if (destination.isAbsolute) {
            destination
        } else {
            layout.projectDirectory.asFile
                .resolve(destination)
        }

    private fun Project.resolveAndCreateDir(
        base: File,
        nestedPath: String? = null,
    ): File {
        val resolved = resolvePath(base).let { if (nestedPath != null) it.resolve(nestedPath) else it }
        resolved.mkdirs()
        return resolved
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
            taskConfig.staticLibrary.set(extension.isStatic)
        }
    }

    private fun Project.configureExportableManifestTask(
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
        taskConfig.staticLibrary.set(extension.isStatic)
    }

    @Suppress("LongParameterList")
    private fun configureGenerateCInteropDefinitionTask(
        task: GenerateCInteropDefinitionTask,
        targetBuildDir: File,
        compiledBinaryName: String,
        cinteropTarget: CompileTarget,
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
        task.staticLibrary.set(extension.isStatic)
    }

    private fun computeOsVersion(
        target: CompileTarget,
        extension: PackageRootDefinitionExtension,
    ): String =
        target.getOsVersion(
            minIos = extension.minIos,
            minWatchos = extension.minWatchos,
            minTvos = extension.minTvos,
            minMacos = extension.minMacos,
        )

    @Suppress("MaxLineLength")
    private fun getBuildMode(extension: PackageRootDefinitionExtension): String = if (extension.debug) "debug" else "release"

    private fun getTargetBuildDirectory(
        packageScratchDir: File,
        cinteropTarget: CompileTarget,
        buildMode: String,
    ): File =
        packageScratchDir
            .resolve(cinteropTarget.getPackageBuildDir())
            .resolve(buildMode)

    // Extracted function to create cinterop tasks
    private fun createCInteropTask(
        mainCompilation: KotlinNativeCompilation,
        cinteropName: String,
        file: File,
    ) {
        mainCompilation.cinterops.create(cinteropName) { settings ->
            settings.definitionFile.set(file)
        }
    }
}
