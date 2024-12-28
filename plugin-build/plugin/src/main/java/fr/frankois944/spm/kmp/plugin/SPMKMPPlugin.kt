@file:OptIn(ExperimentalStdlibApi::class)

package fr.frankois944.spm.kmp.plugin

import fr.frankois944.spm.kmp.plugin.definition.PackageRootDefinitionExtension
import fr.frankois944.spm.kmp.plugin.tasks.CompileSwiftPackageTask
import fr.frankois944.spm.kmp.plugin.tasks.GenerateCInteropDefinitionTask
import fr.frankois944.spm.kmp.plugin.tasks.GenerateManifestTask
import org.gradle.api.Plugin
import org.gradle.api.Project
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

            //   val registerTasks = mutableMapOf<CompileTarget, Task>()

            CompileTarget.entries
                .associateWith {
                    getTaskName(name, it)
                }.forEach { (cinteropTarget, name) ->
                    logger.warn("Register task $name for target $cinteropTarget")

                    project.tasks
                        .register(
                            // name =
                            TASK_GENERATE_MANIFEST + cinteropTarget.toString().capitalized(),
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

                    project.tasks
                        .register(
                            // name =
                            TASK_COMPILE_PACKAGE + cinteropTarget.toString().capitalized(),
                            // type =
                            CompileSwiftPackageTask::class.java,
                            // ...constructorArgs =
                            File(sourcePackageDir, "Package.swift"),
                            cinteropTarget,
                            extension.debug,
                            buildPackageDir,
                            customPackageSource,
                            cinteropTarget.getOsVersion(
                                minIos = extension.minIos,
                                minWatchos = extension.minWatchos,
                                minTvos = extension.minTvos,
                                minMacos = extension.minMacos,
                            ),
                        ).configure {
                            it.dependsOn(
                                project.tasks.withType(GenerateManifestTask::class.java).findByName(
                                    TASK_GENERATE_MANIFEST + cinteropTarget.toString().capitalized(),
                                ),
                            )
                        }

                    project.tasks
                        .register(
                            // name =
                            TASK_GENERATE_CINTEROP_DEF + cinteropTarget.toString().capitalized(),
                            // type =
                            GenerateCInteropDefinitionTask::class.java,
                            // ...constructorArgs =
                            buildPackageDir,
                            cinteropTarget,
                            extension.productName,
                            extension.packages,
                            extension.debug,
                        ).configure {
                            it.dependsOn(
                                project.tasks.withType(CompileSwiftPackageTask::class.java).findByName(
                                    TASK_COMPILE_PACKAGE + cinteropTarget.toString().capitalized(),
                                ),
                            )
                        }
                }

            tasks.withType(CInteropProcess::class.java).configureEach { cinterop ->
                val cinteropTarget =
                    CompileTarget.byKonanName(cinterop.konanTarget.name)
                        ?: return@configureEach

                val generateDefinitionTask =
                    tasks.withType(GenerateCInteropDefinitionTask::class.java).findByName(
                        TASK_GENERATE_CINTEROP_DEF + cinteropTarget.toString().capitalized(),
                    ) ?: return@configureEach

                logger.warn("outputFiles = ${generateDefinitionTask.outputFiles}")
                cinterop.settings.definitionFile.set(generateDefinitionTask.outputFiles[0])
                cinterop.dependsOn(
                    generateDefinitionTask,
                )
            }
        }

    private fun getTaskName(
        cinteropName: String,
        cinteropTarget: CompileTarget,
    ) = "${EXTENSION_NAME}${cinteropName.capitalized()}${cinteropTarget.name.capitalized()}"
}
