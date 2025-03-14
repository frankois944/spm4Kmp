package io.github.frankois944.spmForKmp.tasks.apple

import io.github.frankois944.spmForKmp.config.AppleCompileTarget
import io.github.frankois944.spmForKmp.config.ModuleConfig
import io.github.frankois944.spmForKmp.definition.SwiftDependency
import io.github.frankois944.spmForKmp.operations.getPackageImplicitDependencies
import io.github.frankois944.spmForKmp.operations.getXcodeDevPath
import io.github.frankois944.spmForKmp.tasks.utils.extractModuleNameFromModuleMap
import io.github.frankois944.spmForKmp.tasks.utils.extractPublicHeaderFromCheckout
import io.github.frankois944.spmForKmp.tasks.utils.filterExportableDependency
import io.github.frankois944.spmForKmp.tasks.utils.findHeadersModule
import io.github.frankois944.spmForKmp.tasks.utils.getBuildDirectoriesContent
import io.github.frankois944.spmForKmp.utils.md5
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.konan.target.HostManager
import java.io.File

@CacheableTask
internal abstract class GenerateCInteropDefinitionTask : DefaultTask() {
    init {
        description = "Generate the cinterop definitions files"
        group = "io.github.frankois944.spmForKmp.tasks"
        onlyIf {
            HostManager.hostIsMac
        }
    }

    @get:Input
    abstract val target: Property<AppleCompileTarget>

    @get:Input
    abstract val productName: Property<String>

    @get:Input
    abstract val linkerOpts: ListProperty<String>

    @get:Input
    abstract val compilerOpts: ListProperty<String>

    @get:Input
    abstract val packages: ListProperty<SwiftDependency>

    @get:Input
    abstract val debugMode: Property<Boolean>

    @get:Input
    abstract val osVersion: Property<String>

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val compiledBinary: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val manifestFile: RegularFileProperty

    @get:Input
    abstract val scratchDir: Property<File>

    @get:Input
    @get:Optional
    abstract val packageDependencyPrefix: Property<String?>

    @get:OutputFiles
    val outputFiles: List<File>
        get() =
            buildList {
                getModuleInfos().forEach { moduleName ->
                    add(getBuildDirectory().resolve("${moduleName.name}.def"))
                }
            }

    private fun getBuildDirectory(): File =
        compiledBinary
            .asFile
            .get()
            .parentFile

    private fun getModuleInfos(): List<ModuleConfig> =
        buildList {
            // the first item must be the product name
            add(
                ModuleConfig(
                    name = productName.get(),
                    compilerOpts = compilerOpts.get(),
                    linkerOpts = linkerOpts.get(),
                ),
            )
            addAll(
                packages
                    .get()
                    .filterExportableDependency()
                    .also {
                        logger.debug("Filtered exportable dependency: {}", it)
                    }.flatMap { dependency ->
                        if (dependency is SwiftDependency.Package) {
                            dependency.productsConfig.productPackages
                                .flatMap { product ->
                                    product.products
                                }.map { product ->
                                    ModuleConfig(
                                        name = product.name,
                                        packageName = dependency.packageName,
                                        linkerOpts = product.linkerOpts,
                                        compilerOpts = product.compilerOpts,
                                    )
                                }
                        } else if (dependency is SwiftDependency.Binary) {
                            listOf(
                                ModuleConfig(
                                    name = dependency.packageName,
                                    linkerOpts = dependency.linkerOpts,
                                    compilerOpts = dependency.compilerOpts,
                                ),
                            )
                        } else {
                            listOf(ModuleConfig(name = dependency.packageName))
                        }
                    },
            )
        }.distinctBy { it.name }
            .also {
                logger.debug("Product names to export: {}", it)
            }

    private fun getExtraLinkers(): String {
        val xcodeDevPath = project.getXcodeDevPath()
        return buildList {
            add("-L\"$xcodeDevPath/Toolchains/XcodeDefault.xctoolchain/usr/lib/swift/${target.get().sdk()}\"")
        }.joinToString(" ")
    }

    @Suppress("LongMethod")
    @TaskAction
    fun generateDefinitions() {
        val moduleConfigs = getModuleInfos()

        // find the build directory of the declared module in the manifest
        moduleConfigs
            .forEach { moduleInfo ->
                logger.debug("LOOKING for module dir {}", moduleInfo.name)
                getBuildDirectoriesContent(getBuildDirectory(), "build", "framework")
                    .find {
                        it.nameWithoutExtension.lowercase() == moduleInfo.name.lowercase()
                    }?.let { buildDir ->
                        moduleInfo.isFramework = buildDir.extension == "framework"
                        moduleInfo.buildDir = buildDir
                        moduleInfo.definitionFile = getBuildDirectory().resolve("${moduleInfo.name}.def")
                        logger.debug("Setup module DONE: {}", moduleInfo)
                    }
            }
        logger.debug(
            """
            ModulesConfigs found
            $moduleConfigs
            """.trimIndent(),
        )
        moduleConfigs.forEachIndexed { index, moduleConfig ->
            logger.debug("Building definition file for: {}", moduleConfig)
            try {
                val libName = compiledBinary.asFile.get().name
                val checksum = compiledBinary.asFile.get().md5()
                val mapFile =
                    moduleConfig.buildDir.resolve(
                        if (moduleConfig.isFramework) "Modules/module.modulemap" else "module.modulemap",
                    )
                val mapFileContent = mapFile.readText()
                val moduleName =
                    extractModuleNameFromModuleMap(mapFileContent)
                        ?: throw Exception("No module name for ${moduleConfig.name} in mapFile")

                val definition =
                    if (moduleConfig.isFramework) {
                        generateFrameworkDefinition(moduleName, moduleConfig)
                    } else {
                        generateNonFrameworkDefinition(moduleName, moduleConfig)
                    }.let { def ->
                        // Append staticLibraries for the first index which is the bridge
                        val md5 = "#checksum: $checksum"
                        if (index == 0) "$def\n$md5\nstaticLibraries = $libName" else def
                    }
                if (definition.isNotEmpty()) {
                    moduleConfig.definitionFile.writeText(definition.trimIndent())
                } else {
                    throw RuntimeException("Can't generate definition file")
                }
                logger.debug(
                    """
                    ######
                    Definition File : ${moduleConfig.definitionFile.name}
                    At Path: ${moduleConfig.definitionFile.path}
                    ${moduleConfig.definitionFile.readText()}
                    ######
                    """.trimIndent(),
                )
            } catch (ex: Exception) {
                logger.error(
                    """
                    ######
                    Can't generate definition for ${moduleConfig.name}
                    Expected file: ${moduleConfig.definitionFile.path}
                    Config: $moduleConfig
                    -> Set the `export` parameter to `false` to ignore this module
                    ######
                    """.trimIndent(),
                    ex,
                )
            }
        }
    }

    private fun generateFrameworkDefinition(
        moduleName: String,
        moduleConfig: ModuleConfig,
    ): String {
        val frameworkName = moduleConfig.buildDir.nameWithoutExtension
        val packageName =
            packageDependencyPrefix.orNull?.let {
                "$it.${moduleConfig.name}"
            } ?: moduleConfig.name
        val compilerOpts = moduleConfig.compilerOpts.joinToString(" ")
        val linkerOps = moduleConfig.linkerOpts.joinToString(" ")
        return """
            language = Objective-C
            modules = $moduleName
            package = $packageName
            libraryPaths = "${getBuildDirectory().path}"
            compilerOpts = $compilerOpts -fmodules -framework "$frameworkName" -F"${getBuildDirectory().path}"
            linkerOpts = $linkerOps ${getExtraLinkers()} -framework "$frameworkName" -F"${getBuildDirectory().path}"
            """.trimIndent()
    }

    private fun generateNonFrameworkDefinition(
        moduleName: String,
        moduleConfig: ModuleConfig,
    ): String {
        val implicitDependencies =
            project
                .getPackageImplicitDependencies(
                    workingDir = manifestFile.asFile.get().parentFile,
                    scratchPath = scratchDir.get(),
                ).getFolders()

        val headerSearchPaths =
            buildList {
                addAll(extractPublicHeaderFromCheckout(scratchDir.get(), moduleConfig))
                addAll(getBuildDirectoriesContent(getBuildDirectory(), "build"))
                addAll(implicitDependencies)
                addAll(findHeadersModule(scratchDir.get().resolve("artifacts"), target.get()))
            }.joinToString(" ") { "-I\"$it\"" }

        val packageName =
            packageDependencyPrefix.orNull?.let {
                "$it.${moduleConfig.name}"
            } ?: moduleConfig.name
        val compilerOpts = moduleConfig.compilerOpts.joinToString(" ")
        val linkerOps = moduleConfig.linkerOpts.joinToString(" ")
        return """
            language = Objective-C
            modules = $moduleName
            package = $packageName
            libraryPaths = "${getBuildDirectory().path}"
            compilerOpts = $compilerOpts -fmodules $headerSearchPaths -F"${getBuildDirectory().path}"
            linkerOpts = $linkerOps ${getExtraLinkers()} -F"${getBuildDirectory().path}"
            """.trimIndent()
    }
}
