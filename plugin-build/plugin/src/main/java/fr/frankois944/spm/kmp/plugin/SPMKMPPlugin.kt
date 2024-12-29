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
                val dependencyTasks = mutableMapOf<CompileTarget, MutableList<String>>()

                tasks.withType(CInteropProcess::class.java).forEach { cinterop ->
                    val cinteropTarget =
                        CompileTarget.byKonanName(cinterop.konanTarget.name)
                            ?: return@forEach

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

                    val dependenciesFiles = task3.get().outputFiles.drop(1)
                    // link the dependencies definition files
                    if (dependenciesFiles.isNotEmpty()) {
                        dependencyTasks[cinteropTarget] = mutableListOf()
                        val ktTarget =
                            kotlinExtension.targets.findByName(cinteropTarget.name) as? KotlinNativeTarget
                        val mainCompilation = ktTarget!!.compilations.getByName("main")
                        dependenciesFiles.forEach { file ->
                            val taskName = getTaskName(file.nameWithoutExtension.capitalized())
                            mainCompilation.cinterops.create(
                                taskName,
                            ) { settings ->
                                settings.defFile(file)
                            }
                            dependencyTasks[cinteropTarget]?.add(taskName)
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

                // link the main definition File
                tasks.withType(CInteropProcess::class.java).configureEach { cinterop ->
                    val cinteropTarget =
                        CompileTarget.byKonanName(cinterop.konanTarget.name)
                            ?: return@configureEach

                    val definitionTask =
                        taskGroup[cinteropTarget] as GenerateCInteropDefinitionTask

                    if (definitionTask.outputFiles.isEmpty()) {
                        logger.error("No definition files found, can't use cinterop")
                    }
                    logger.debug("outputFiles = {}", definitionTask.outputFiles)
                    cinterop.settings.definitionFile.set(definitionTask.outputFiles.first())

                    dependencyTasks[cinteropTarget]?.let {
                        it.forEach { name ->
                            tasks.getByName("cinterop$name${cinteropTarget.name.capitalized()}").dependsOn(taskGroup[cinteropTarget])
                            tasks.getByName("cinterop$name${cinteropTarget.name.capitalized()}").mustRunAfter(taskGroup[cinteropTarget])
                        }
                    }
                    cinterop.dependsOn(taskGroup[cinteropTarget])
                }
            }
        }

    private fun getTaskName(
        task: String,
        cinteropTarget: CompileTarget? = null,
    ) = "${EXTENSION_NAME.capitalized()}${task.capitalized()}${cinteropTarget?.name?.capitalized() ?: ""}"
}
