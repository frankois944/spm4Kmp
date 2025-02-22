@file:OptIn(ExperimentalStdlibApi::class)

package io.github.frankois944.spmForKmp

import io.github.frankois944.spmForKmp.config.AppleCompileTarget
import io.github.frankois944.spmForKmp.definition.PackageRootDefinitionExtension
import io.github.frankois944.spmForKmp.tasks.apple.configAppleTargets
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.reflect.TypeOf
import org.jetbrains.kotlin.gradle.tasks.CInteropProcess
import java.io.File
import kotlin.reflect.javaType
import kotlin.reflect.typeOf

internal const val PLUGIN_NAME: String = "swiftPackageConfig"
internal const val SWIFT_PACKAGE_NAME = "Package.swift"
internal const val TASK_GENERATE_MANIFEST: String = "generateSwiftPackage"
internal const val TASK_COMPILE_PACKAGE: String = "compileSwiftPackage"
internal const val TASK_GENERATE_CINTEROP_DEF: String = "generateCInteropDefinition"
internal const val TASK_GENERATE_EXPORTABLE_PACKAGE: String = "generateExportableSwiftPackage"

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
                swiftPackageEntries.forEach { extension ->

                    val sourcePackageDir =
                        resolveAndCreateDir(
                            extension.spmWorkingPath?.let { File(it) }
                                ?: layout.buildDirectory.asFile.get(),
                            "spmKmpPlugin",
                            extension.name
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
                    configAppleTargets(
                        taskGroup = taskGroup,
                        cInteropTaskNamesWithDefFile = cInteropTaskNamesWithDefFile,
                        extension = extension,
                        sourcePackageDir = sourcePackageDir,
                        packageScratchDir = packageScratchDir,
                        sharedCacheDir = sharedCacheDir,
                        sharedConfigDir = sharedConfigDir,
                        sharedSecurityDir = sharedSecurityDir,
                        swiftSourcePackageDir = swiftSourcePackageDir,
                    )
                }
                // link the main definition File
                tasks.withType(CInteropProcess::class.java).configureEach { cinterop ->
                    val cinteropTarget =
                        AppleCompileTarget.byKonanName(cinterop.konanTarget.name)
                            ?: return@configureEach
                    // The cinterop task needs to run the requirement tasks before getting the .def file
                    cinterop.dependsOn(taskGroup[cinteropTarget])
                    cinterop.mustRunAfter(taskGroup[cinteropTarget])
                    val definitionFile = cInteropTaskNamesWithDefFile[cinterop.name]
                    cinterop.settings.definitionFile.set(definitionFile)
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
}
