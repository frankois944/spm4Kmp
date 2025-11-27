@file:OptIn(ExperimentalStdlibApi::class)

package io.github.frankois944.spmForKmp

import io.github.frankois944.spmForKmp.config.AppleCompileTarget
import io.github.frankois944.spmForKmp.config.PackageDirectoriesConfig
import io.github.frankois944.spmForKmp.definition.PackageRootDefinitionExtension
import io.github.frankois944.spmForKmp.tasks.checkExistCInteropTask
import io.github.frankois944.spmForKmp.tasks.configAppleTargets
import io.github.frankois944.spmForKmp.tasks.createCInteropTask
import io.github.frankois944.spmForKmp.utils.getAndCreateFakeDefinitionFile
import org.gradle.api.GradleException
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.reflect.TypeOf
import org.gradle.internal.extensions.stdlib.capitalized
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.tasks.CInteropProcess
import org.jetbrains.kotlin.konan.target.HostManager
import java.io.File
import kotlin.reflect.javaType
import kotlin.reflect.typeOf

internal const val PLUGIN_NAME: String = "swiftPackageConfig"
internal const val SWIFT_PACKAGE_NAME = "Package.swift"
internal const val TASK_GENERATE_MANIFEST: String = "generateSwiftPackage"
internal const val TASK_COMPILE_PACKAGE: String = "compileSwiftPackage"
internal const val TASK_GENERATE_CINTEROP_DEF: String = "generateCInteropDefinition"
internal const val TASK_GENERATE_EXPORTABLE_PACKAGE: String = "generateExportableSwiftPackage"
internal const val TASK_COPY_PACKAGE_RESOURCES: String = "CopyPackageResources"

@Suppress("UnnecessaryAbstractClass")
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
                val entries = swiftPackageEntries + project.swiftContainer()
                createMissingCinteropTask(entries)
                mergeEntries(entries).forEach { swiftPackageEntry ->
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

                    tasks
                        .withType(CInteropProcess::class.java)
                        .forEach {
                            logger.warn("CURRENT CInteropProcess task found: $it")
                        }

                    configAppleTargets(
                        taskGroup = taskGroup,
                        cInteropTaskNamesWithDefFile = cInteropTaskNamesWithDefFile,
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
                        taskGroup[cinteropTarget]?.let {
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

    private fun Project.createMissingCinteropTask(swiftPackageEntry: Set<PackageRootDefinitionExtension>) {
        swiftPackageEntry.forEach { entry ->
            if (!entry.useExtension) {
                return
            }
            entry.targetName?.let { targetName ->
                val ktTarget =
                    extensions
                        .getByType(KotlinMultiplatformExtension::class.java)
                        .targets
                        .findByName(targetName) as KotlinNativeTarget
                val mainCompilationTarget = ktTarget.compilations.getByName("main")
                if (!checkExistCInteropTask(mainCompilationTarget, entry.internalName.capitalized())) {
                    createCInteropTask(
                        mainCompilationTarget,
                        cinteropName = entry.internalName.capitalized(),
                        file = getAndCreateFakeDefinitionFile(),
                    )
                }
            }
        }
    }

    private fun mergeEntries(entries: Set<PackageRootDefinitionExtension>): Set<PackageRootDefinitionExtension> {
        val mergedEntries = mutableSetOf<PackageRootDefinitionExtension>()
        entries.forEach { entry ->
            val foundEntry =
                mergedEntries
                    .firstOrNull { mergedEntry ->
                        mergedEntry.internalName == entry.internalName
                    }
            if (foundEntry == null) {
                mergedEntries.add(entry)
            }
        }
        return mergedEntries
    }
}
