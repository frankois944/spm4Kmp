package io.github.frankois944.spmForKmp.tasks.apple

import io.github.frankois944.spmForKmp.config.AppleCompileTarget
import io.github.frankois944.spmForKmp.config.ModuleConfig
import io.github.frankois944.spmForKmp.definition.SwiftDependency
import io.github.frankois944.spmForKmp.operations.getXcodeDevPath
import io.github.frankois944.spmForKmp.tasks.utils.filterExportableDependency
import io.github.frankois944.spmForKmp.utils.md5
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.konan.target.HostManager
import java.io.File
import java.nio.file.Path
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.pathString

@CacheableTask
internal abstract class GenerateCInteropDefinitionTask : DefaultTask() {
    init {
        onlyIf {
            HostManager.hostIsMac
        }
    }

    private companion object {
        const val DEBUG_PREFIX = "Debug-"
        const val RELEASE_PREFIX = "Release-"
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
    abstract val compiledBinaryName: Property<String>

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val manifestFile: RegularFileProperty

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val buildWorkingDir: DirectoryProperty

    @get:Input
    @get:Optional
    abstract val packageDependencyPrefix: Property<String?>

    init {
        description = "Generate the cinterop definitions files"
        group = "io.github.frankois944.spmForKmp.tasks"
    }

    private val definitionDirectoryPath: Path
        get() =
            buildWorkingDir
                .get()
                .asFile
                .resolve("Definitions")
                .toPath()

    private val productDirectory: Path
        get() =
            buildWorkingDir
                .get()
                .asFile
                .resolve("Build")
                .resolve("Products")
                .resolve(getProductSubPath())
                .toPath()

    private fun getProductSubPath(): String {
        val buildTypePrefix = if (debugMode.get()) DEBUG_PREFIX else RELEASE_PREFIX
        return buildTypePrefix + target.get().sdk()
    }

    private val moduleMapDirectory: Path
        get() =
            buildWorkingDir
                .get()
                .asFile
                .resolve("Build")
                .resolve("Intermediates.noindex")
                .resolve("GeneratedModuleMaps-${target.get().sdk()}")
                .toPath()

    @get:OutputFiles
    val outputFiles: List<File>
        get() =
            buildList {
                getModuleInfos().forEach { moduleName ->
                    add(
                        definitionDirectoryPath.resolve("${moduleName.name}.def").toFile(),
                    )
                }
            }

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
                moduleMapDirectory.find { moduleMap ->
                    moduleMap.nameWithoutExtension.lowercase() == moduleInfo.name.lowercase()
                }
                // moduleInfo.isFramework = buildDir.extension == "framework"
                // moduleInfo.buildDir = buildDir
                moduleInfo.definitionFile = definitionDirectoryPath.resolve("${moduleInfo.name}.def").toFile()
                logger.debug("Setup module DONE: {}", moduleInfo)
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
                val finalBinaryFile = productDirectory.resolve(compiledBinaryName.get()).toFile()
                if (!finalBinaryFile.exists()) {
                    throw RuntimeException("Cant find the binary output of the bridge ${finalBinaryFile.absolutePath}")
                }
                val libName = finalBinaryFile.nameWithoutExtension
                val checksum = finalBinaryFile.md5()

                val definition =
                    """
language = Objective-C
modules = $libName
package = $libName
libraryPaths = ${moduleMapDirectory.pathString}
compilerOpts = -fmodules -I"${moduleMapDirectory.pathString}" -L"${moduleMapDirectory.pathString}" -F"${moduleMapDirectory.pathString}"
linkerOpts = -l:${finalBinaryFile.path} ${getExtraLinkers()} -L"${moduleMapDirectory.pathString}" -F"${moduleMapDirectory.pathString}"
                    """.trimIndent()

                /* val mapFile =
                     moduleConfig.buildDir.resolve(
                         if (moduleConfig.isFramework) "Modules/module.modulemap" else "module.modulemap",
                     )
                 val mapFileContent = mapFile.readText()
                 val moduleName =
                     extractModuleNameFromModuleMap(mapFileContent)
                         ?: throw Exception("No module name for ${moduleConfig.name} in mapFile")*/

                /*val definition =
                    if (moduleConfig.isFramework) {
                        generateFrameworkDefinition(moduleName, moduleConfig)
                    } else {
                        generateNonFrameworkDefinition(moduleName, moduleConfig)
                    }.let { def ->
                        // Append staticLibraries for the first index which is the bridge
                        val md5 = "#checksum: $checksum"
                        if (index == 0) "$def\n$md5\nstaticLibraries = $libName" else def
                    }*/
                if (definition.isNotEmpty()) {
                    moduleConfig.definitionFile.writeText(definition)
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

    /* private fun generateFrameworkDefinition(
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
         /*val implicitDependencies =
             project
                 .getPackageImplicitDependencies(
                     workingDir = manifestFile.asFile.get().parentFile,
                     scratchPath = packageWorkingDir.get(),
                 ).getFolders()*/

         val headerSearchPaths =
             buildList {
                 addAll(extractPublicHeaderFromCheckout(packageWorkingDir.get(), moduleConfig))
                 addAll(getBuildDirectoriesContent(getBuildDirectory(), "build"))
                 // addAll(implicitDependencies)
                 addAll(findHeadersModule(packageWorkingDir.get().resolve("artifacts"), target.get()))
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
     }*/
}
