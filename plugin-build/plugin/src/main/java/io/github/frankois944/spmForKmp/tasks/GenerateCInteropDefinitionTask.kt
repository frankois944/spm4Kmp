package io.github.frankois944.spmForKmp.tasks

import io.github.frankois944.spmForKmp.CompileTarget
import io.github.frankois944.spmForKmp.config.ModuleConfig
import io.github.frankois944.spmForKmp.definition.SwiftDependency
import io.github.frankois944.spmForKmp.definition.helpers.filterExportableDependency
import io.github.frankois944.spmForKmp.definition.product.ProductName
import io.github.frankois944.spmForKmp.operations.getPackageImplicitDependencies
import io.github.frankois944.spmForKmp.operations.getXcodeDevPath
import io.github.frankois944.spmForKmp.utils.md5
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.konan.target.HostManager
import java.io.File

internal abstract class GenerateCInteropDefinitionTask : DefaultTask() {
    init {
        onlyIf {
            HostManager.hostIsMac
        }
    }

    @get:Input
    abstract val target: Property<CompileTarget>

    @get:Input
    abstract val productName: Property<String>

    @get:Input
    abstract val packages: ListProperty<SwiftDependency>

    @get:Input
    abstract val debugMode: Property<Boolean>

    @get:Input
    abstract val osVersion: Property<String>

    @get:InputFile
    abstract val compiledBinary: RegularFileProperty

    @get:InputFile
    abstract val manifestFile: RegularFileProperty

    @get:Input
    abstract val scratchDir: Property<File>

    init {
        description = "Generate the cinterop definitions files"
        group = "io.github.frankois944.spmForKmp.tasks"
    }

    @get:OutputFiles
    val outputFiles: List<File>
        get() =
            buildList {
                getModuleNames().forEach { moduleName ->
                    add(getBuildDirectory().resolve("$moduleName.def"))
                }
            }

    private fun getBuildDirectory(): File =
        compiledBinary
            .asFile
            .get()
            .parentFile

    private fun getBuildDirectoriesContent(vararg extensions: String): List<File> =
        getBuildDirectory() // get folders with headers for internal dependencies
            .listFiles { file -> extensions.contains(file.extension) || file.name == "Modules" }
            // remove folder with weird names, cinterop doesn't like module with symbol names like grp-c++
            // it doesn't matter for the kotlin export, to be rethinking
            ?.filter { file -> !file.nameWithoutExtension.lowercase().contains("grpc") }
            ?.toList()
            .orEmpty()

    private fun extractModuleNameFromModuleMap(module: String): String? {
        /*
         * find a better regex to extract the module value
         */
        val regex = """module\s+(\w+)""".toRegex()
        return regex
            .find(module)
            ?.groupValues
            ?.firstOrNull()
            ?.replace("module", "")
            ?.trim()
    }

    private fun extractHeaderPathFromModuleMap(module: String): File? {
        /*
         * find a better regex to extract the header value
         */
        val regex = """header\s+"([^"]+)"""".toRegex()
        return regex
            .find(module)
            ?.groupValues
            ?.firstOrNull()
            ?.replace("header", "")
            ?.replace("\"", "")
            ?.trim()
            ?.let { File(it) }
    }

    private fun getModuleNames(): List<String> =
        buildList {
            add(productName.get()) // the first item must be the product name
            addAll(
                packages
                    .get()
                    .filterExportableDependency()
                    .also {
                        logger.debug("Filtered exportable dependency: {}", it)
                    }.flatMap { dependency ->
                        if (dependency is SwiftDependency.Package) {
                            val productList: List<ProductName> =
                                @Suppress("RedundantHigherOrderMapUsage")
                                dependency.productsConfig.productPackages.flatMap { product ->
                                    product.products.map { it }
                                }
                            val namesList = productList.map { product -> product.name }
                            namesList
                        } else {
                            listOf(dependency.packageName)
                        }
                    }.also {
                        logger.debug("Product names to export: {}", it)
                    },
            )
        }.distinct()

    /**
     * Constructs and returns a string of linker flags and options specific to the build configuration.
     *
     * The method determines the appropriate linker platform version name or minimum OS version name
     * based on the Xcode version. It combines various flags such as platform version, OS version,
     * runtime path, and library path for the generated binary.
     *
     * @return A string of linker flags and options constructed based on the build configuration.
     */
    private fun getExtraLinkers(): String {
        val xcodeDevPath = project.getXcodeDevPath()
        return buildList {
            add("-L\"$xcodeDevPath/Toolchains/XcodeDefault.xctoolchain/usr/lib/swift/${target.get().sdk()}\"")
        }.joinToString(" ")
    }

    @Suppress("LongMethod")
    @TaskAction
    fun generateDefinitions() {
        val moduleConfigs = mutableListOf<ModuleConfig>()
        val moduleNames = getModuleNames()

        logger.debug(
            """
            moduleNames
            $moduleNames
            """.trimIndent(),
        )

        // find the build directory of the declared module in the manifest
        moduleNames
            .forEach { moduleName ->
                logger.debug("LOOKING for module dir {}", moduleName)
                getBuildDirectoriesContent("build", "framework")
                    .find {
                        it.nameWithoutExtension.lowercase() == moduleName.lowercase()
                    }?.let { buildDir ->
                        logger.debug("Found dir {} for {}", buildDir, moduleName)
                        moduleConfigs.add(
                            ModuleConfig(
                                isFramework = buildDir.extension == "framework",
                                name = moduleName,
                                buildDir = buildDir,
                                definitionFile = getBuildDirectory().resolve("$moduleName.def"),
                            ),
                        )
                    }
            }
        logger.debug(
            """
            modulesConfigs found
            $moduleConfigs
            """.trimIndent(),
        )
        moduleConfigs.forEachIndexed { index, moduleConfig ->
            logger.debug("Building definition file for: {}", moduleConfig)
            var definition = ""
            try {
                val libName = compiledBinary.asFile.get().name
                val checksum = compiledBinary.asFile.get().md5()
                if (moduleConfig.isFramework) {
                    val mapFile = moduleConfig.buildDir.resolve("Modules").resolve("module.modulemap")
                    val moduleName =
                        extractModuleNameFromModuleMap(mapFile.readText())
                            ?: throw Exception("No module name from ${moduleConfig.name} in mapFile")
                    definition = """
language = Objective-C
modules = $moduleName
package = ${moduleConfig.name}
# Set a checksum for avoid build cache
# checkum: $checksum
libraryPaths = "${getBuildDirectory().path}"
compilerOpts = -fmodules -framework "${moduleConfig.buildDir.nameWithoutExtension}" -F"${getBuildDirectory().path}"
linkerOpts = ${getExtraLinkers()} -framework "${moduleConfig.buildDir.nameWithoutExtension}" -F"${getBuildDirectory().path}"
                        """
                } else {
                    val mapFile = moduleConfig.buildDir.resolve("module.modulemap")
                    val mapFileContent = mapFile.readText()
                    val moduleName =
                        extractModuleNameFromModuleMap(mapFileContent)
                            ?: throw RuntimeException("No module name from ${moduleConfig.name} in mapFile")
                    val implicitDependencies =
                        project
                            .getPackageImplicitDependencies(
                                workingDir = manifestFile.asFile.get().parentFile,
                                scratchPath = scratchDir.get(),
                            ).getFolders()
                    val headersBuildPath =
                        buildList {
                            addAll(getBuildDirectoriesContent("build"))
                            addAll(implicitDependencies)
                            extractHeaderPathFromModuleMap(mapFileContent)?.let {
                                add(it)
                            }
                        }.joinToString(" ") { "-I\"${it}\"" }
                    definition =
                        """
language = Objective-C
modules = $moduleName
package = ${moduleConfig.name}
# Set a checksum for avoid build cache
# checkum: $checksum
libraryPaths = "${getBuildDirectory().path}"
compilerOpts = -fmodules $headersBuildPath -F"${getBuildDirectory().path}"
linkerOpts = ${getExtraLinkers()} -F"${getBuildDirectory().path}"
                        """
                }
                if (index == 0) {
                    definition = """
$definition
staticLibraries = $libName
                            """
                }
                if (definition.isNotEmpty()) {
                    moduleConfig.definitionFile.writeText(definition.trimIndent())
                }
                logger.debug(
                    """
                    ######
                    Definition File : ${moduleConfig.definitionFile.name}
                    At Path: ${moduleConfig.definitionFile.path}
                    ${moduleConfig.definitionFile.readText()}moduleConfig.definitionFile.readText()}
                    ######
                    """.trimIndent(),
                )
            } catch (ex: Exception) {
                logger.error(
                    """
                    Can't generate definition for ${moduleConfig.name}")
                    Expected file ${moduleConfig.definitionFile.path}
                    CONTENT ${moduleConfig.definitionFile.readText()}
                    -> Set the `export` parameter to `false` to ignore this module
                    """.trimIndent(),
                    ex,
                )
            }
        }
    }
}
