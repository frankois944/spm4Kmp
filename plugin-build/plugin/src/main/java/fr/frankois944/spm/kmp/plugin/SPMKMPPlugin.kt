@file:OptIn(ExperimentalStdlibApi::class)

package fr.frankois944.spm.kmp.plugin

import fr.frankois944.spm.kmp.plugin.definition.PackageRootDefinitionExtension
import fr.frankois944.spm.kmp.plugin.tasks.CompileSwiftPackageTask
import fr.frankois944.spm.kmp.plugin.tasks.GenerateCInteropDefinitionTask
import fr.frankois944.spm.kmp.plugin.tasks.GenerateManifestTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.internal.extensions.stdlib.capitalized
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.tasks.CInteropProcess
import org.jetbrains.kotlin.konan.target.HostManager
import java.io.File

internal const val EXTENSION_NAME: String = "swiftPackageConfig"
internal const val TASK_GENERATE_MANIFEST: String = "generateSwiftPackage"
internal const val TASK_COMPILE_PACKAGE: String = "compileSwiftPackage"
internal const val TASK_GENERATE_CINTEROP_DEF: String = "generateCInteropDefinition"
internal const val TASK_LINK_CINTEROP_DEF: String = "linkCInteropDefinition"

@Suppress("UnnecessaryAbstractClass")
public abstract class SPMKMPPlugin : Plugin<Project> {
    private fun Project.resolvePath(destination: File): File =
        if (destination.startsWith("/")) {
            destination
        } else {
            project.layout.projectDirectory.asFile
                .resolve(destination)
        }

    override fun apply(target: Project): Unit =
        with(target) {
            if (!HostManager.hostIsMac) {
                logger.error("The plugin SPMKMPPlugin can only run on macos")
                return
            }

            // the plugin extension configuration
            val extension = extensions.create(EXTENSION_NAME, PackageRootDefinitionExtension::class.java, project)

            // load the multiplatform extension and configuration
            plugins.apply("org.jetbrains.kotlin.multiplatform")
            val kotlinExtension = project.extensions.getByName("kotlin") as KotlinMultiplatformExtension

            val sourcePackageDir =
                layout.buildDirectory.asFile
                    .get()
                    .resolve("spmKmpPlugin")
                    .resolve("input")
                    .also {
                        it.mkdirs()
                    }
            val buildPackageDir =
                layout.buildDirectory.asFile
                    .get()
                    .resolve("spmKmpPlugin")
                    .resolve("output")
                    .also {
                        it.mkdirs()
                    }

            afterEvaluate {
                val originalPackageScratchDir =
                    resolvePath(sourcePackageDir)
                        .resolve("scratch")
                        .also {
                            it.mkdirs()
                        }

                val userSourcePackageDir =
                    resolvePath(File(extension.customPackageSourcePath))
                        .also {
                            it.mkdirs()
                        }

                val task1 =
                    tasks
                        .register(
                            // name =
                            TASK_GENERATE_MANIFEST,
                            // type =
                            GenerateManifestTask::class.java,
                            // ...constructorArgs =
                            extension.packages,
                            extension.productName,
                            extension.minIos,
                            extension.minTvos,
                            extension.minMacos,
                            extension.minWatchos,
                            extension.toolsVersion,
                            sourcePackageDir,
                            originalPackageScratchDir,
                        )

                val taskGroup = mutableMapOf<CompileTarget, Task>()
                val dependencyTaskNames = mutableMapOf<String, File>()

                CompileTarget.entries.forEach { cinteropTarget ->
                    logger.warn("INIT CInteropProcess ${cinteropTarget.name}")

                    val targetPackageScratchDir =
                        buildPackageDir
                            .resolve(cinteropTarget.name)
                            .also {
                                if (!it.exists()) {
                                    it.mkdirs()
                                }
                            }

                    val task2 =
                        tasks
                            .register(
                                // name =
                                getTaskName(TASK_COMPILE_PACKAGE, cinteropTarget),
                                // type =
                                CompileSwiftPackageTask::class.java,
                                // ...constructorArgs =
                                File(sourcePackageDir, "Package.swift"),
                                cinteropTarget,
                                extension.debug,
                                originalPackageScratchDir,
                                targetPackageScratchDir,
                                userSourcePackageDir,
                                cinteropTarget.getOsVersion(
                                    minIos = extension.minIos,
                                    minWatchos = extension.minWatchos,
                                    minTvos = extension.minTvos,
                                    minMacos = extension.minMacos,
                                ),
                            )

                    val task3 =
                        tasks
                            .register(
                                // name =
                                getTaskName(TASK_GENERATE_CINTEROP_DEF, cinteropTarget),
                                // type =
                                GenerateCInteropDefinitionTask::class.java,
                                // ...constructorArgs =
                                targetPackageScratchDir,
                                cinteropTarget,
                                extension.productName,
                                extension.packages,
                                extension.debug,
                                cinteropTarget.getOsVersion(
                                    minIos = extension.minIos,
                                    minWatchos = extension.minWatchos,
                                    minTvos = extension.minTvos,
                                    minMacos = extension.minMacos,
                                ),
                            )

                    val dependenciesFiles = task3.get().outputFiles
                    if (dependenciesFiles.isNotEmpty()) {
                        val ktTarget = kotlinExtension.targets.findByName(cinteropTarget.name) as? KotlinNativeTarget
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
                                val fullTaskName = getCInteropTaskName(file.nameWithoutExtension, cinteropTarget)
                                dependencyTaskNames[fullTaskName] = file
                            }
                        }

                        taskGroup[cinteropTarget] =
                            task3
                                .get()
                                .dependsOn(
                                    task2
                                        .get()
                                        .dependsOn(task1.get()),
                                )
                    }
                }

                // link the main definition File
                tasks.withType(CInteropProcess::class.java).configureEach { cinterop ->
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
    ) = "${EXTENSION_NAME.capitalized()}${task.capitalized()}${cinteropTarget?.name?.capitalized() ?: ""}"

    private fun getCInteropTaskName(
        name: String,
        cinteropTarget: CompileTarget?,
    ): String =
        buildString {
            append("cinterop${name.capitalized()}${cinteropTarget?.name?.capitalized() ?: ""}")
        }
}
