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
import org.jetbrains.kotlin.gradle.tasks.CInteropProcess
import org.jetbrains.kotlin.konan.target.HostManager
import java.io.File
import kotlin.io.path.Path
import kotlin.io.path.createDirectories

internal const val EXTENSION_NAME: String = "swiftPackageConfig"
internal const val TASK_GENERATE_MANIFEST: String = "generateSwiftPackage"
internal const val TASK_COMPILE_PACKAGE: String = "compileSwiftPackage"
internal const val TASK_GENERATE_CINTEROP_DEF: String = "generateCInteropDefinition"

@Suppress("UnnecessaryAbstractClass")
public abstract class SPMKMPPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit =
        with(target) {
            if (!HostManager.hostIsMac) {
                println("The plugin spm-kmp can only run on macos")
                return
            }

            val extension =
                project.extensions.create(EXTENSION_NAME, PackageRootDefinitionExtension::class.java, project)
            val sourcePackageDir =
                project.layout.buildDirectory.asFile
                    .get()
                    .resolve("spmKmpPlugin")
                    .resolve("input")
                    .also {
                        it.mkdirs()
                    }
            val buildPackageDir =
                project.layout.buildDirectory.asFile
                    .get()
                    .resolve("spmKmpPlugin")
                    .resolve("output")
                    .also {
                        it.mkdirs()
                    }
            val customPackageSource =
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

            var taskGroup = mutableMapOf<CompileTarget, Task>()

            CompileTarget.entries.forEach { cinteropTarget ->
                val buildDir = buildPackageDir.resolve(cinteropTarget.name)
                if (!buildDir.exists()) {
                    buildDir.mkdirs()
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
                            buildDir,
                            customPackageSource,
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
                            buildDir,
                            cinteropTarget,
                            extension.productName,
                            extension.packages,
                            extension.debug,
                        )

                task2.get().mustRunAfter(task1.get())
                task3.get().mustRunAfter(task2.get())
                taskGroup[cinteropTarget] =
                    task3
                        .get()
                        .dependsOn(task2.get())
                        .dependsOn(task1.get())
            }

            var previousTarget: CompileTarget? = null
            tasks.withType(CInteropProcess::class.java).configureEach { cinterop ->
                val cinteropTarget =
                    CompileTarget.byKonanName(cinterop.konanTarget.name)
                        ?: return@configureEach

                val definitionTask =
                    taskGroup[cinteropTarget] as GenerateCInteropDefinitionTask

                logger.warn("outputFiles = ${definitionTask.outputFiles}")
                cinterop.settings.definitionFile.set(definitionTask.outputFiles[0])
                previousTarget?.let {
                    taskGroup[cinteropTarget]?.mustRunAfter(taskGroup[previousTarget])
                }
                cinterop.dependsOn(taskGroup[cinteropTarget])
                previousTarget = cinteropTarget
            }
        }

    private fun getTaskName(
        task: String,
        cinteropTarget: CompileTarget,
    ) = "${task.capitalized()}${cinteropTarget.name.capitalized()}"
}
