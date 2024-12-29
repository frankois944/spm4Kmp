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
import org.jetbrains.kotlin.gradle.plugin.mpp.DefaultCInteropSettings
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.tasks.CInteropProcess
import org.jetbrains.kotlin.konan.target.HostManager
import java.io.File
import kotlin.io.path.Path
import kotlin.io.path.createDirectories

internal const val EXTENSION_NAME: String = "swiftPackageConfig"
internal const val TASK_GENERATE_MANIFEST: String = "generateSwiftPackage"
internal const val TASK_COMPILE_PACKAGE: String = "compileSwiftPackage"
internal const val TASK_GENERATE_CINTEROP_DEF: String = "generateCInteropDefinition"
internal const val TASK_LINK_CINTEROP_DEF: String = "linkCInteropDefinition"

@Suppress("UnnecessaryAbstractClass")
public abstract class SPMKMPPlugin : Plugin<Project> {
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
                val userPackagePath =
                    Path(extension.customPackageSourcePath)
                        .also {
                            it.createDirectories()
                        }.toFile()

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
                            File(sourcePackageDir, "Package.swift"),
                        )

                val taskGroup = mutableMapOf<CompileTarget, Task>()
                val dependencyTasks = mutableMapOf<CompileTarget, DefaultCInteropSettings>()

                CompileTarget.entries.forEach { cinteropTarget ->
                    val packageScratchPath = buildPackageDir.resolve(cinteropTarget.name)
                    if (!buildPackageDir.exists()) {
                        packageScratchPath.mkdirs()
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
                                packageScratchPath,
                                if (userPackagePath.startsWith("/")) {
                                    userPackagePath
                                } else {
                                    layout.projectDirectory.asFile.resolve(userPackagePath)
                                },
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
                                packageScratchPath,
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

                    /*
                 val konanTargets = tasks.withType(CInteropProcess::class.java).asSequence().map { it.konanTarget }
                    konanTargets.forEach {
                        val compileTarget =
                            CompileTarget.byKonanName(it.name)!!
                        logger.warn("Ctask type = {}", compileTarget)
                        // the first def file is the product one, ignore it

                        }
                     */

                    taskGroup[cinteropTarget] =
                        task3
                            .get()
                            .dependsOn(
                                task2
                                    .get()
                                    .dependsOn(task1.get()),
                            )
                }

                afterEvaluate {
                    // link the dependencies definition files
                    taskGroup.forEach { (target, task) ->
                        val dependenciesFiles = (task as GenerateCInteropDefinitionTask).outputFiles.drop(1)
                        if (dependenciesFiles.isNotEmpty()) {
                            val ktTarget =
                                kotlinExtension.targets.findByName(target.name) as? KotlinNativeTarget
                                    ?: return@forEach
                            val mainCompilation = ktTarget.compilations.getByName("main")
                            dependenciesFiles.forEach { file ->
                                val taskName = getTaskName(file.nameWithoutExtension.capitalized(), target)
                                logger.debug("Create task name = {}", taskName)
                                dependencyTasks[target] =
                                    mainCompilation.cinterops.create(
                                        taskName,
                                    ) { task ->
                                        task.defFile(file)
                                    }
                            }
                        }
                    }
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
                    cinterop.dependsOn(taskGroup[cinteropTarget])
                }
            }
        }

    private fun getTaskName(
        task: String,
        cinteropTarget: CompileTarget,
    ) = "${EXTENSION_NAME.capitalized()}${task.capitalized()}${cinteropTarget.name.capitalized()}"
}
