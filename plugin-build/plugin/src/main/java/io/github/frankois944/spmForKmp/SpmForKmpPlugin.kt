@file:OptIn(ExperimentalStdlibApi::class)

package io.github.frankois944.spmForKmp

import io.github.frankois944.spmForKmp.config.AppleCompileTarget
import io.github.frankois944.spmForKmp.config.NewPublicationInteroperabilityFeature
import io.github.frankois944.spmForKmp.config.PackageDirectoriesConfig
import io.github.frankois944.spmForKmp.definition.PackageRootDefinitionExtension
import io.github.frankois944.spmForKmp.tasks.checkExistCInteropTask
import io.github.frankois944.spmForKmp.tasks.configAppleTargets
import io.github.frankois944.spmForKmp.tasks.createCInteropTask
import io.github.frankois944.spmForKmp.tasks.utils.disableStartupFile
import io.github.frankois944.spmForKmp.utils.StartingFile
import io.github.frankois944.spmForKmp.utils.compareVersions
import io.github.frankois944.spmForKmp.utils.getAndCreateFakeDefinitionFile
import org.gradle.api.GradleException
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.reflect.TypeOf
import org.gradle.internal.extensions.stdlib.capitalized
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.kotlinToolingVersion
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.tasks.CInteropProcess
import org.jetbrains.kotlin.konan.target.HostManager
import java.io.File
import kotlin.reflect.javaType
import kotlin.reflect.typeOf

internal const val PLUGIN_NAME: String = "swiftPackageConfig"
internal const val SWIFT_PACKAGE_NAME = "Package.swift"
internal const val SWIFT_PACKAGE_RESOLVE_NAME = "Package.resolved"
internal const val TASK_GENERATE_MANIFEST: String = "generateSwiftPackage"
internal const val TASK_COMPILE_PACKAGE: String = "compileSwiftPackage"
internal const val TASK_GENERATE_CINTEROP_DEF: String = "generateCInteropDefinition"
internal const val TASK_GENERATE_EXPORTABLE_PACKAGE: String = "generateExportableSwiftPackage"
internal const val TASK_GENERATE_REGISTRY_FILE: String = "generateRegistryFilePackage"
internal const val TASK_COPY_PACKAGE_RESOURCES: String = "CopyPackageResources"
internal const val SPM_TRACE_NAME: String = "spmForKmpTrace"

@Suppress("UnnecessaryAbstractClass", "CyclomaticComplexMethod")
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

            afterEvaluate {
                // Contains the group of task (with their dependency) by target
                val taskGroup = mutableMapOf<AppleCompileTarget, Task>()
                // Contains the cinterop .def file linked with the task name
                val cInteropTaskNamesWithDefFile = mutableMapOf<String, File>()
                // Contains the definition producer task linked with the cinterop task name
                val cInteropTaskNamesWithProducerTask = mutableMapOf<String, Task>()
                // Contains the exported-package task linked with the cinterop task name
                val cInteropTaskNamesWithExportTask = mutableMapOf<String, Task>()
                val entries = (swiftPackageEntries + project.swiftContainer()).toSet()
                createMissingCinteropTask(entries)
                if (entries.any { entry -> entry.newPublicationInteroperabilityFeature }) {
                    logger.warn(
                        "Caution: experimental interoperability mode is enabled " +
                            "(https://kotlinlang.org/docs/whatsnew2320.html#new-interoperability-" +
                            "mode-for-c-or-objective-c-libraries)",
                    )
                }
                mergeEntries(entries).forEach { swiftPackageEntry ->
                    if (!swiftPackageEntry.useExtension) {
                        logger.warn(
                            """
                            Please migrate your configuration to use the extension way (https://spmforkmp.eu/usages/multiTarget/)
                            This will be a error in the future
                            """.trimIndent(),
                        )
                    }

                    val spmWorkingDir =
                        resolveAndCreateDir(
                            File(swiftPackageEntry.spmWorkingPath),
                            "spmKmpPlugin",
                            swiftPackageEntry.internalName,
                        )
                    val packageScratchDir = resolveAndCreateDir(spmWorkingDir, "scratch")
                    val sharedCacheDir = swiftPackageEntry.sharedCachePath?.let { resolveAndCreateDir(File(it)) }
                    val sharedConfigDir = swiftPackageEntry.sharedConfigPath?.let { resolveAndCreateDir(File(it)) }
                    val sharedSecurityDir = swiftPackageEntry.sharedSecurityPath?.let { resolveAndCreateDir(File(it)) }
                    val bridgeSourceDir =
                        resolveAndCreateDir(
                            File(swiftPackageEntry.customPackageSourcePath),
                            swiftPackageEntry.internalName,
                        )

                    if (!project.disableStartupFile()) {
                        StartingFile.createStartingFileIfNeeded(bridgeSourceDir)
                    }

                    tasks
                        .withType(CInteropProcess::class.java)
                        .forEach {
                            logger.debug("CInteropProcess task found: {}", it)
                        }

                    configAppleTargets(
                        taskGroup = taskGroup,
                        cInteropTaskNamesWithDefFile = cInteropTaskNamesWithDefFile,
                        cInteropTaskNamesWithProducerTask = cInteropTaskNamesWithProducerTask,
                        cInteropTaskNamesWithExportTask = cInteropTaskNamesWithExportTask,
                        swiftPackageEntry = swiftPackageEntry,
                        packageDirectoriesConfig =
                            PackageDirectoriesConfig(
                                spmWorkingDir = spmWorkingDir,
                                packageScratchDir = packageScratchDir,
                                sharedCacheDir = sharedCacheDir,
                                sharedConfigDir = sharedConfigDir,
                                sharedSecurityDir = sharedSecurityDir,
                                bridgeSourceDir = bridgeSourceDir,
                            ),
                    )
                }

                // link the main definition File
                tasks.withType(CInteropProcess::class.java).configureEach { cinterop ->
                    if (HostManager.hostIsMac) {
                        val cinteropTarget =
                            AppleCompileTarget.fromKonanTarget(cinterop.konanTarget)
                                ?: return@configureEach
                        val producerTask = cInteropTaskNamesWithProducerTask[cinterop.name]
                        val exportTask = cInteropTaskNamesWithExportTask[cinterop.name]
                        val targetRootTask = taskGroup[cinteropTarget]
                        producerTask?.let {
                            cinterop.dependsOn(producerTask)
                            cinterop.mustRunAfter(producerTask)
                            exportTask?.let { task ->
                                cinterop.dependsOn(task)
                                cinterop.mustRunAfter(task)
                            }
                        } ?: targetRootTask?.let {
                            cinterop.dependsOn(taskGroup[cinteropTarget])
                            cinterop.mustRunAfter(taskGroup[cinteropTarget])
                        } ?: run {
                            // If there is no task, there is something really wrong somewhere.
                            // the user must make an issue with its configuration
                            throw GradleException(
                                """
                                spmForKmp failed :
                                No task found for target ${cinteropTarget.name}
                                make an issue with your plugin configuration
                                """.trimIndent(),
                            )
                        }
                        val definitionFile = cInteropTaskNamesWithDefFile[cinterop.name]
                        // Note: the definitionFile doesn't exist yet, but we know where it will be.
                        cinterop.settings.definitionFile.set(definitionFile)
                    } else {
                        cinterop.settings.definitionFile.set(getAndCreateFakeDefinitionFile())
                    }
                }
            }
        }

    private fun Project.resolvePath(destination: File): File =
        if (destination.isAbsolute) {
            destination
        } else {
            layout.projectDirectory.asFile
                .resolve(destination)
        }

    private fun Project.resolveAndCreateDir(
        base: File,
        vararg nestedPath: String = emptyArray(),
    ): File {
        var resolved = resolvePath(base)
        nestedPath.forEach { resolved = resolved.resolve(it) }
        resolved.mkdirs()
        return resolved
    }

    @Suppress("NestedBlockDepth")
    private fun Project.createMissingCinteropTask(swiftPackageEntry: Set<PackageRootDefinitionExtension>) {
        swiftPackageEntry.forEach { entry ->
            if (!entry.useExtension) {
                return@forEach
            }
            entry.targetName?.let { targetName ->
                val ktTarget =
                    extensions
                        .getByType(KotlinMultiplatformExtension::class.java)
                        .targets
                        .findByName(targetName) as KotlinNativeTarget
                val mainCompilationTarget = ktTarget.compilations.getByName("main")
                if (!checkExistCInteropTask(mainCompilationTarget, entry.internalName.capitalized())) {
                    val extraOpts = mutableListOf<String>()
                    if (entry.newPublicationInteroperabilityFeature &&
                        compareVersions(
                            kotlinToolingVersion.toString(),
                            NewPublicationInteroperabilityFeature.minKotlinVersion(),
                        ) >= 0
                    ) {
                        extraOpts.addAll(NewPublicationInteroperabilityFeature.extraOpts())
                    }
                    createCInteropTask(
                        mainCompilationTarget,
                        cinteropName = entry.internalName.capitalized(),
                        file = getAndCreateFakeDefinitionFile(),
                        extraOpts = extraOpts,
                    )
                }
            }
        }
    }

    private fun mergeEntries(entries: Set<PackageRootDefinitionExtension>): Set<PackageRootDefinitionExtension> =
        entries
            .groupBy { it.internalName }
            .values
            .map { groupedEntries ->
                groupedEntries.firstOrNull { it.targetName == null } ?: groupedEntries.first()
            }.toSet()
}
