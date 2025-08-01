package io.github.frankois944.spmForKmp.tasks.apple

import io.github.frankois944.spmForKmp.config.ModuleConfig
import io.github.frankois944.spmForKmp.definition.SwiftDependency
import io.github.frankois944.spmForKmp.definition.exported.ExportedPackage
import io.github.frankois944.spmForKmp.manifest.TemplateParameters
import io.github.frankois944.spmForKmp.manifest.generateManifest
import io.github.frankois944.spmForKmp.operations.isDynamicLibrary
import io.github.frankois944.spmForKmp.operations.swiftFormat
import io.github.frankois944.spmForKmp.utils.getPlistValue
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.jetbrains.kotlin.konan.target.HostManager
import java.io.File
import javax.inject.Inject

@CacheableTask
internal abstract class GenerateExportableManifestTask : DefaultTask() {
    @get:Input
    abstract val packageDependencies: ListProperty<SwiftDependency>

    @get:Input
    abstract val packageName: Property<String>

    @get:Input
    @get:Optional
    abstract val minIos: Property<String?>

    @get:Input
    @get:Optional
    abstract val minMacos: Property<String?>

    @get:Input
    @get:Optional
    abstract val minTvos: Property<String?>

    @get:Input
    @get:Optional
    abstract val minWatchos: Property<String?>

    @get:Input
    abstract val toolsVersion: Property<String>

    @get:Input
    abstract val exportedPackage: Property<ExportedPackage>

    @get:OutputFile
    abstract val manifestFile: RegularFileProperty

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val compiledTargetDir: DirectoryProperty

    @get:Input
    abstract val includeProduct: ListProperty<String>

    @get:OutputFile
    val exportedSource: File
        get() =
            manifestFile
                .get()
                .asFile
                .parentFile
                .resolve("Sources")
                .resolve("DummySPMFile.swift")
                .also {
                    it.parentFile.mkdirs()
                    it.writeText(
                        """
                        // This file has been generated by Spm4Kmp plugin
                        // DO NO EDIT THIS FILE AS IT WILL BE OVERWRITTEN ON EACH BUILD
                        import Foundation
                        """.trimIndent(),
                    )
                }

    @get:Inject
    abstract val execOps: ExecOperations

    private fun getCurrentModules(): List<ModuleConfig> =
        packageDependencies
            .get()
            .flatMap { dependency ->
                when (dependency) {
                    is SwiftDependency.Package -> {
                        dependency.productsConfig.productPackages
                            .flatMap { product ->
                                product.products
                            }.map { product ->
                                ModuleConfig(
                                    name = product.name,
                                    packageName = dependency.packageName,
                                    spmPackageName = dependency.packageName,
                                    swiftDependency = dependency,
                                )
                            }
                    }

                    is SwiftDependency.Binary -> {
                        listOf(
                            ModuleConfig(
                                name = dependency.packageName,
                                spmPackageName = dependency.packageName,
                                swiftDependency = dependency,
                            ),
                        )
                    }
                }
            }

    init {
        description = "Generate a Swift Package manifest with exported product"
        group = "io.github.frankois944.spmForKmp.tasks"
        onlyIf {
            HostManager.hostIsMac
        }
    }

    @Suppress("LongMethod")
    @TaskAction
    fun generateFile() {
        val requiredDependencies =
            getRequireProductsDependencies()

        if (requiredDependencies.isNotEmpty()) {
            val manifest =
                generateManifest(
                    parameters =
                        TemplateParameters(
                            forExportedPackage = true,
                            dependencies = packageDependencies.get(),
                            generatedPackageDirectory =
                                manifestFile
                                    .get()
                                    .asFile.parentFile
                                    .toPath(),
                            productName = packageName.get(),
                            minIos = minIos.orNull.orEmpty(),
                            minMacos = minMacos.orNull.orEmpty(),
                            minTvos = minTvos.orNull.orEmpty(),
                            minWatchos = minWatchos.orNull.orEmpty(),
                            toolsVersion = toolsVersion.get(),
                            targetSettings = null,
                            exportedPackage = exportedPackage.get(),
                            onlyDeps = requiredDependencies,
                        ),
                )
            manifestFile.asFile.get().writeText(manifest)
            try {
                execOps.swiftFormat(
                    manifestFile.asFile.get(),
                    logger,
                )
                val namesToExport = requiredDependencies.joinToString(",") { it.name }
                logger.lifecycle(
                    """
                    Spm4Kmp: The following dependencies [$namesToExport] need to be added to your xcode project
                    A local Swift package has been generated at
                    ${manifestFile.get().asFile.parentFile.path}
                    Please add it to your xcode project as a local package dependency; it will add the missing content.
                    ****You can ignore this messaging if you have already added these dependencies to your Xcode project****
                    """.trimIndent(),
                )
            } catch (ex: Exception) {
                logger.error(
                    "Manifest file generated : \n{}\n{}",
                    manifestFile.get().asFile,
                    manifestFile.get().asFile.readText(),
                )
                throw ex
            }
        } else {
            logger.debug(
                "No dependencies to export found; delete the old one {}",
                manifestFile.asFile.get().absolutePath,
            )
            manifestFile.asFile
                .get()
                .parentFile
                .deleteRecursively()
        }
    }

    // the static framework must be included inside the xcode project
    @Suppress("NestedBlockDepth")
    private fun getRequireProductsDependencies(): List<ModuleConfig> {
        val requireDependencies = mutableListOf<ModuleConfig>()
        val modules = getCurrentModules()
        logger.debug("ALL MODULE {}", modules)
        val moduleToInclude = includeProduct.get().map { it.lowercase() }
        modules.forEach { module ->
            if (moduleToInclude.contains(module.name.lowercase())) {
                requireDependencies.add(module)
            } else {
                compiledTargetDir
                    .asFile
                    .get()
                    .listFiles { it.extension == "framework" }
                    .firstOrNull {
                        logger.debug("checking module {} at {}", module.name, it)
                        it.nameWithoutExtension.lowercase() == module.name.lowercase()
                    }?.let { moduleLocation ->
                        // if the module is inside the compiled build directory
                        val plist = moduleLocation.resolve("Info.plist")
                        logger.debug("Looking inside the Info.plist {}", plist)
                        val libraryName = getPlistValue(plist, "CFBundleExecutable")
                        logger.debug("Found libraryName {}", libraryName)
                        val binaryFile = moduleLocation.resolve(libraryName)
                        if (!execOps.isDynamicLibrary(binaryFile, logger)) {
                            logger.debug(
                                "Found static framework {} add it to the require dependency list",
                                moduleLocation,
                            )
                            requireDependencies.add(module)
                        }
                    }
            }
        }
        return requireDependencies
    }
}
