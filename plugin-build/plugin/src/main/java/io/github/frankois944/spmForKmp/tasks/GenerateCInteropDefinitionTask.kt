package io.github.frankois944.spmForKmp.tasks

import io.github.frankois944.spmForKmp.config.CompileTarget
import io.github.frankois944.spmForKmp.config.ModuleConfig
import io.github.frankois944.spmForKmp.config.ModuleInfo
import io.github.frankois944.spmForKmp.definition.SwiftDependency
import io.github.frankois944.spmForKmp.definition.helpers.filterExportableDependency
import io.github.frankois944.spmForKmp.operations.getPackageImplicitDependencies
import io.github.frankois944.spmForKmp.operations.getXcodeDevPath
import io.github.frankois944.spmForKmp.utils.extractTargetBlocks
import io.github.frankois944.spmForKmp.utils.md5
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
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

    @get:Input
    @get:Optional
    abstract val packageDependencyPrefix: Property<String?>

    init {
        description = "Generate the cinterop definitions files"
        group = "io.github.frankois944.spmForKmp.tasks"
    }

    @get:OutputFiles
    val outputFiles: List<File>
        get() =
            buildList {
                getModuleNames().forEach { moduleName ->
                    add(getBuildDirectory().resolve("${moduleName.name}.def"))
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

    private fun extractPublicHeaderFromCheckout(module: ModuleConfig): Set<File> {
        logger.debug("Looking for public header for ${module.name}")

        val checkoutsDir = "checkouts"
        val packageSwift = "Package.swift"
        val sourcesDir = "Sources"

        val packageDir = scratchDir.get().resolve(checkoutsDir).resolve(module.packageName)
        val manifestFile = packageDir.resolve(packageSwift)
        val result = mutableSetOf<File>()

        if (!manifestFile.exists()) {
            logger.debug("No manifest found at ${manifestFile.path}")
            return result
        }

        val content = manifestFile.readText()
        val targets = extractTargetBlocks(content)

        targets.forEach { target ->
            val name = extractFirstMatch(target, """name:\s*"([^"]+)"""")
            val targetPath = extractFirstMatch(target, """path:\s*"([^"]+)"""")
            logger.debug("targetPath: $targetPath")
            val publicHeadersPath = extractFirstMatch(target, """publicHeadersPath:\s*"([^"]+)"""")
            logger.debug("publicHeadersPath: $publicHeadersPath")
            var resolvedIncludeDir =
                if (targetPath != null) {
                    packageDir.resolve(targetPath)
                } else {
                    packageDir.resolve(sourcesDir).resolve(name ?: module.name)
                }

            if (publicHeadersPath != null) {
                resolvedIncludeDir = resolvedIncludeDir.resolve(publicHeadersPath)
            }

            logger.debug("resolvedIncludeDir: {}", resolvedIncludeDir)
            if (resolvedIncludeDir.exists()) {
                result.add(resolvedIncludeDir)
            } else {
                logger.debug("PUBLIC HEADER NOT FOUND AT: {} FOR : {}", resolvedIncludeDir, module.name)
            }
        }
        return result
    }

    private fun extractFirstMatch(
        input: String,
        pattern: String,
    ): String? = Regex(pattern).find(input)?.groupValues?.getOrNull(1)

    private fun extractModuleNameFromModuleMap(module: String): String? {
        val regex = """module\s+\S+\s+""".toRegex()
        return regex
            .find(module)
            ?.groupValues
            ?.firstOrNull()
            ?.replace("module", "")
            ?.trim()
            ?.also {
                logger.debug("MODULE FOUND {}", it)
            }
    }

    private fun getModuleNames(): List<ModuleInfo> =
        buildList {
            add(ModuleInfo(productName.get())) // the first item must be the product name
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
                                    product.products.map { it }
                                }.map { product ->
                                    ModuleInfo(product.name, dependency.packageName)
                                }
                        } else {
                            listOf(ModuleInfo(dependency.packageName))
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
                        it.nameWithoutExtension.lowercase() == moduleName.name.lowercase()
                    }?.let { buildDir ->
                        logger.debug("build dir {} for {}", buildDir, moduleName)
                        moduleConfigs.add(
                            ModuleConfig(
                                isFramework = buildDir.extension == "framework",
                                name = moduleName.name,
                                buildDir = buildDir,
                                packageName = moduleName.packageName,
                                definitionFile = getBuildDirectory().resolve("${moduleName.name}.def"),
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
                    throw RuntimeException("Can't generate defintion file")
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
        return """
language = Objective-C
modules = $moduleName
package = $packageName
libraryPaths = "${getBuildDirectory().path}"
compilerOpts = -fmodules -framework "$frameworkName" -F"${getBuildDirectory().path}"
linkerOpts = ${getExtraLinkers()} -framework "$frameworkName" -F"${getBuildDirectory().path}"
    """
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
                addAll(extractPublicHeaderFromCheckout(moduleConfig))
                addAll(getBuildDirectoriesContent("build"))
                addAll(implicitDependencies)
            }.joinToString(" ") { "-I\"$it\"" }

        val packageName =
            packageDependencyPrefix.orNull?.let {
                "$it.${moduleConfig.name}"
            } ?: moduleConfig.name
        return """
language = Objective-C
modules = $moduleName
package = $packageName
libraryPaths = "${getBuildDirectory().path}"
compilerOpts = -fmodules $headerSearchPaths -F"${getBuildDirectory().path}"
linkerOpts = ${getExtraLinkers()} -F"${getBuildDirectory().path}"
    """
    }
}
