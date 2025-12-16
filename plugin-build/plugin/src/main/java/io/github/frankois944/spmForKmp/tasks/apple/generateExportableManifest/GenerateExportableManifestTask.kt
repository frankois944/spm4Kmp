package io.github.frankois944.spmForKmp.tasks.apple.generateExportableManifest

import io.github.frankois944.spmForKmp.config.ModuleConfig
import io.github.frankois944.spmForKmp.definition.SwiftDependency
import io.github.frankois944.spmForKmp.definition.exported.ExportedPackage
import io.github.frankois944.spmForKmp.manifest.TemplateParameters
import io.github.frankois944.spmForKmp.manifest.generateManifest
import io.github.frankois944.spmForKmp.operations.isDynamicLibrary
import io.github.frankois944.spmForKmp.tasks.utils.TaskTracer
import io.github.frankois944.spmForKmp.utils.getPlistValue
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
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
    abstract val minIos: Property<String>

    @get:Input
    @get:Optional
    abstract val minMacos: Property<String>

    @get:Input
    @get:Optional
    abstract val minTvos: Property<String>

    @get:Input
    @get:Optional
    abstract val minWatchos: Property<String>

    @get:Input
    abstract val toolsVersion: Property<String>

    @get:Input
    abstract val exportedPackage: Property<ExportedPackage>

    @get:Input
    abstract val compiledTargetDir: Property<String>

    @get:Input
    abstract val includeProduct: ListProperty<String>

    @get:Input
    abstract val traceEnabled: Property<Boolean>

    @get:OutputFile
    abstract val storedTraceFile: Property<File>

    @get:Input
    abstract val hideLocalPackageMessage: Property<Boolean>

    @get:OutputDirectory
    abstract val exportedDirectory: DirectoryProperty

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
                                )
                            }
                    }

                    is SwiftDependency.Binary -> {
                        listOf(
                            ModuleConfig(
                                name = dependency.packageName,
                                spmPackageName = dependency.packageName,
                                swiftDependency = dependency,
                                isCLang = dependency.isCLang,
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
        val tracer =
            TaskTracer(
                "GenerateExportableManifestTask",
                traceEnabled.get(),
                outputFile = storedTraceFile.get(),
            )
        val manifestFile = exportedDirectory.get().asFile.resolve("Package.swift")
        tracer.trace("GenerateManifestTask") {
            prepareExportedPackage()
            val requiredDependencies =
                tracer.trace("getRequireProductsDependencies") {
                    getRequireProductsDependencies()
                }
            if (requiredDependencies.isNotEmpty()) {
                tracer.trace("generateManifest") {
                    val manifest =
                        generateManifest(
                            parameters =
                                TemplateParameters(
                                    forExportedPackage = true,
                                    dependencies = packageDependencies.get(),
                                    generatedPackageDirectory = exportedDirectory.get().asFile.toPath(),
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
                    manifestFile.writeText(manifest)
                }
                tracer.trace("swiftFormat") {
                    try {
                        if (!hideLocalPackageMessage.get()) {
                            val namesToExport = requiredDependencies.joinToString(",") { it.name }
                            logger.error(
                                """
                                Spm4Kmp: The following dependencies [$namesToExport] need to be added to your xcode project
                                A local Swift package has been generated at
                                ${exportedDirectory.get().asFile.path}
                                Please add it to your xcode project as a local package dependency
                                Check https://spmforkmp.eu/bridgeWithDependencies/#automatic-dependency-build-inclusion for more details
                                Set "spmforkmp.hideLocalPackageMessage=true" inside gradle.properties to hide this message
                                """.trimIndent(),
                            )
                        }
                    } catch (ex: Exception) {
                        logger.error(
                            "Manifest file generated : \n{}\n{}",
                            manifestFile,
                            manifestFile.readText(),
                        )
                        throw ex
                    }
                }
            } else {
                logger.debug(
                    "No dependencies to export found; delete the old one {}",
                    exportedDirectory.get().asFile.absolutePath,
                )
                exportedDirectory
                    .get()
                    .asFile
                    .deleteRecursively()
            }
        }
        tracer.writeHtmlReport()
    }

    private fun prepareExportedPackage() {
        exportedDirectory
            .get()
            .asFile
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
    }

    // the static framework must be included inside the xcode project
    @Suppress("NestedBlockDepth")
    private fun getRequireProductsDependencies(): List<ModuleConfig> {
        val requireDependencies = mutableListOf<ModuleConfig>()
        val modules = getCurrentModules()
        logger.debug("ALL MODULE {}", modules)
        val moduleToInclude = includeProduct.get().map { it.lowercase() }
        modules.forEach { module ->
            if (module.isCLang) {
                requireDependencies.add(module)
            } else if (moduleToInclude.contains(module.name.lowercase())) {
                requireDependencies.add(module)
            } else {
                File(compiledTargetDir.get())
                    .listFiles { it.extension == "framework" }
                    .firstOrNull {
                        logger.debug("checking module {} at {}", module.name, it)
                        it.nameWithoutExtension.equals(module.name, ignoreCase = true)
                    }?.let { moduleLocation ->
                        // if the module is inside the compiled build directory
                        var plist = moduleLocation.resolve("Info.plist")
                        if (!plist.exists()) {
                            logger.debug(
                                "The plist is not at the root of the framework, try the Resource folder instead",
                            )
                            plist = moduleLocation.resolve("Resources").resolve("Info.plist")
                        }
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
