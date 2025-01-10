@file:OptIn(ExperimentalStdlibApi::class)

package io.github.frankois944.spmForKmp

import io.github.frankois944.spmForKmp.definition.PackageRootDefinitionExtension
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
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.tasks.CInteropProcess
import org.jetbrains.kotlin.konan.target.HostManager
import java.io.File
import kotlin.reflect.javaType
import kotlin.reflect.typeOf

internal const val EXTENSION_NAME: String = "swiftPackageConfig"
internal const val TASK_GENERATE_MANIFEST: String = "generateSwiftPackage"
internal const val TASK_COMPILE_PACKAGE: String = "compileSwiftPackage"
internal const val TASK_GENERATE_CINTEROP_DEF: String = "generateCInteropDefinition"
internal const val TASK_GENERATE_EXPORTABLE_PACKAGE: String = "generateExportableSwiftPackage"

@Suppress("UnnecessaryAbstractClass")
public abstract class SpmForKmpPlugin : Plugin<Project> {
    private fun Project.resolvePath(destination: File): File =
        if (destination.isAbsolute) {
            destination
        } else {
            project.layout.projectDirectory.asFile
                .resolve(destination)
        }

    @Suppress("LongMethod", "CyclomaticComplexMethod")
    override fun apply(target: Project): Unit =
        with(target) {
            if (!HostManager.hostIsMac) {
                logger.error("The plugin SPMKMPPlugin can only run on macos")
                return
            }

            // check if the multiplatform plugin is loaded
            if (!plugins.hasPlugin("org.jetbrains.kotlin.multiplatform")) {
                throw RuntimeException("The plugin SPMKMPPlugin requires the kotlin multiplatform plugin")
            }

            val swiftPackageEntries: NamedDomainObjectContainer<out PackageRootDefinitionExtension> =
                objects.domainObjectContainer(PackageRootDefinitionExtension::class.java) { name ->
                    objects.newInstance(PackageRootDefinitionExtension::class.java, name)
                }

            val type =
                TypeOf.typeOf<NamedDomainObjectContainer<out PackageRootDefinitionExtension>>(
                    typeOf<NamedDomainObjectContainer<PackageRootDefinitionExtension>>().javaType,
                )

            project.extensions.add(type, EXTENSION_NAME, swiftPackageEntries)

            val kotlinExtension =
                project.extensions.getByName("kotlin") as KotlinMultiplatformExtension

            val sourcePackageDir =
                layout.buildDirectory.asFile
                    .get()
                    .resolve("spmKmpPlugin")
                    .also {
                        it.mkdirs()
                    }

            val packageScratchDir =
                resolvePath(sourcePackageDir)
                    .resolve("scratch")
                    .also {
                        it.mkdirs()
                    }
            afterEvaluate {
                val extension = swiftPackageEntries.first()
                if (swiftPackageEntries.size > 1) {
                    logger.warn("Only the first entry is currently supported, the other will be ignored")
                }

                val sharedCacheDir: File? =
                    extension.sharedCachePath?.run {
                        resolvePath(File(this))
                            .also { dir ->
                                dir.mkdirs()
                            }
                    }

                val sharedConfigDir: File? =
                    extension.sharedConfigPath?.run {
                        resolvePath(File(this))
                            .also { dir ->
                                dir.mkdirs()
                            }
                    }

                val sharedSecurityDir: File? =
                    extension.sharedSecurityPath?.run {
                        resolvePath(File(this))
                            .also { dir ->
                                dir.mkdirs()
                            }
                    }

                val userSourcePackageDir =
                    resolvePath(File(extension.customPackageSourcePath))
                        .also { dir ->
                            dir.mkdirs()
                        }

                val task1 =
                    tasks
                        .register(
                            // name =
                            getTaskName(TASK_GENERATE_MANIFEST),
                            // type =
                            GenerateManifestTask::class.java,
                        ) { manifest ->
                            manifest.packageDependencies.set(extension.packageDependencies)
                            manifest.packageName.set(extension.name)
                            manifest.minIos.set(extension.minIos)
                            manifest.minTvos.set(extension.minTvos)
                            manifest.minMacos.set(extension.minMacos)
                            manifest.minWatchos.set(extension.minWatchos)
                            manifest.toolsVersion.set(extension.toolsVersion)
                            manifest.manifestFile.set(sourcePackageDir.resolve("Package.swift"))
                            manifest.packageScratchDir.set(packageScratchDir)
                            manifest.sharedCacheDir.set(sharedCacheDir)
                            manifest.sharedConfigDir.set(sharedConfigDir)
                            manifest.sharedSecurityDir.set(sharedSecurityDir)
                        }
                val exportablePackage = extension.packageDependencies.filter { it.exportToKotlin }
                val task4: TaskProvider<GenerateExportableManifestTask>? =
                    if (exportablePackage.isNotEmpty()) {
                        tasks
                            .register(
                                // name =
                                getTaskName(TASK_GENERATE_EXPORTABLE_PACKAGE),
                                // type =
                                GenerateExportableManifestTask::class.java,
                            ) { manifest ->
                                manifest.packageDependencies.set(exportablePackage)
                                manifest.packageName.set("exported${extension.name.capitalized()}")
                                manifest.minIos.set(extension.minIos)
                                manifest.minTvos.set(extension.minTvos)
                                manifest.minMacos.set(extension.minMacos)
                                manifest.minWatchos.set(extension.minWatchos)
                                manifest.toolsVersion.set(extension.toolsVersion)
                                val manifestDir =
                                    project.layout.projectDirectory.asFile
                                        .resolve("exported${extension.name.capitalized()}")
                                        .also { it.mkdirs() }
                                manifest.manifestFile.set(manifestDir.resolve("Package.swift"))
                            }
                    } else {
                        null
                    }

                val taskGroup = mutableMapOf<CompileTarget, Task>()
                val dependencyTaskNames = mutableMapOf<String, File>()

                CompileTarget.entries.forEach { cinteropTarget ->
                    val targetBuildDir =
                        packageScratchDir
                            .resolve(cinteropTarget.getPackageBuildDir())
                            .resolve(if (extension.debug) "debug" else "release")

                    val task2 =
                        tasks
                            .register(
                                // name =
                                getTaskName(TASK_COMPILE_PACKAGE, cinteropTarget),
                                // type =
                                CompileSwiftPackageTask::class.java,
                            ) {
                                it.manifestFile.set(File(sourcePackageDir, "Package.swift"))
                                it.target.set(cinteropTarget)
                                it.debugMode.set(extension.debug)
                                it.packageScratchDir.set(packageScratchDir)
                                it.compiledTargetDir.set(targetBuildDir)
                                it.customSourcePackage.set(userSourcePackageDir)
                                it.osVersion.set(
                                    cinteropTarget.getOsVersion(
                                        minIos = extension.minIos,
                                        minWatchos = extension.minWatchos,
                                        minTvos = extension.minTvos,
                                        minMacos = extension.minMacos,
                                    ),
                                )
                                it.sharedCacheDir.set(sharedCacheDir)
                                it.sharedConfigDir.set(sharedConfigDir)
                                it.sharedSecurityDir.set(sharedSecurityDir)
                            }

                    val task3 =
                        tasks
                            .register(
                                // name =
                                getTaskName(TASK_GENERATE_CINTEROP_DEF, cinteropTarget),
                                // type =
                                GenerateCInteropDefinitionTask::class.java,
                            ) {
                                it.compiledBinary.set(targetBuildDir.resolve("lib${extension.name}.a"))
                                it.target.set(cinteropTarget)
                                it.productName.set(extension.name)
                                it.packages.set(extension.packageDependencies)
                                it.debugMode.set(extension.debug)
                                it.osVersion.set(
                                    cinteropTarget.getOsVersion(
                                        minIos = extension.minIos,
                                        minWatchos = extension.minWatchos,
                                        minTvos = extension.minTvos,
                                        minMacos = extension.minMacos,
                                    ),
                                )
                                it.manifestFile.set(sourcePackageDir.resolve("Package.swift"))
                                it.scratchDir.set(packageScratchDir)
                            }

                    val dependenciesFiles = task3.get().outputFiles
                    if (dependenciesFiles.isNotEmpty()) {
                        val ktTarget =
                            kotlinExtension.targets.findByName(cinteropTarget.name) as? KotlinNativeTarget
                        if (ktTarget != null) {
                            val mainCompilation = ktTarget.compilations.getByName("main")
                            dependenciesFiles.forEachIndexed { index, file ->
                                if (index > 0) {
                                    // create cinterop tasks for the dependencies
                                    mainCompilation.cinterops.create(
                                        file.nameWithoutExtension,
                                    ) { settings ->
                                        settings.definitionFile.set(file)
                                    }
                                }
                                // store the cinterop task name for retrieving the file later
                                val fullTaskName =
                                    getCInteropTaskName(file.nameWithoutExtension, cinteropTarget)
                                dependencyTaskNames[fullTaskName] = file
                            }
                        }

                        taskGroup[cinteropTarget] =
                            task3
                                .get()
                                .dependsOn(
                                    task2
                                        .get()
                                        .dependsOn(listOfNotNull(task1.get(), task4?.get())),
                                )
                    }
                }

                // link the main definition File
                tasks.withType(CInteropProcess::class.java).configureEach { cinterop ->
                    cinterop.onlyIf {
                        plugins.hasPlugin("org.jetbrains.kotlin.multiplatform")
                    }
                    val cinteropTarget =
                        CompileTarget.byKonanName(cinterop.konanTarget.name)
                            ?: return@configureEach
                    cinterop.dependsOn(taskGroup[cinteropTarget])
                    val definitionFile = dependencyTaskNames[cinterop.name]
                    cinterop.settings.definitionFile.set(definitionFile)
                }
            }
        }

    private fun getTaskName(
        task: String,
        cinteropTarget: CompileTarget? = null,
    ) = "${EXTENSION_NAME.capitalized()}${task.capitalized()}${
        cinteropTarget?.name?.capitalized().orEmpty()
    }"

    private fun getCInteropTaskName(
        name: String,
        cinteropTarget: CompileTarget?,
    ): String =
        buildString {
            append("cinterop${name.capitalized()}${cinteropTarget?.name?.capitalized().orEmpty()}")
        }
}
