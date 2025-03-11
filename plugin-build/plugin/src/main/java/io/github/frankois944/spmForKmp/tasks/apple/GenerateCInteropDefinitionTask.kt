package io.github.frankois944.spmForKmp.tasks.apple

import io.github.frankois944.spmForKmp.config.AppleCompileTarget
import io.github.frankois944.spmForKmp.config.ModuleConfig
import io.github.frankois944.spmForKmp.definition.SwiftDependency
import io.github.frankois944.spmForKmp.operations.getPackageImplicitDependencies
import io.github.frankois944.spmForKmp.operations.getXcodeDevPath
import io.github.frankois944.spmForKmp.tasks.utils.extractPublicHeaderFromCheckout
import io.github.frankois944.spmForKmp.tasks.utils.filterExportableDependency
import io.github.frankois944.spmForKmp.tasks.utils.findHeadersModule
import io.github.frankois944.spmForKmp.tasks.utils.getDirectories
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
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.konan.target.HostManager
import java.io.File
import kotlin.io.path.copyTo

@CacheableTask
internal abstract class GenerateCInteropDefinitionTask : DefaultTask() {
    init {
        description = "Generate the cinterop definitions files"
        group = "io.github.frankois944.spmForKmp.tasks.apple"
        onlyIf {
            HostManager.hostIsMac
        }
    }

    private companion object {
        const val DEBUG_PREFIX = "Debug"
        const val RELEASE_PREFIX = "Release"
    }

    @get:Input
    abstract val target: Property<AppleCompileTarget>

    @get:Internal
    abstract val clonedSourcePackages: Property<File>

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

    @get:Internal
    abstract val buildWorkingDir: Property<File>

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val builtBridgeSwiftSource: DirectoryProperty

    @get:Input
    @get:Optional
    abstract val packageDependencyPrefix: Property<String?>

    @get:OutputFiles
    val definitionFiles: List<File>
        get() =
            buildList {
                getModuleInfos().forEach { moduleName ->
                    add(
                        definitionDirectoryPath.resolve("${moduleName.name}.def"),
                    )
                }
            }

    @get:OutputDirectory
    val cinteropModulePath: File
        get() =
            buildWorkingDir
                .get()
                .resolve("Modules")
                .resolve(target.get().getPackageBuildDir())

    @Suppress("LongMethod")
    @TaskAction
    fun generateDefinitions() {
        val moduleConfigs = getModuleInfos()

        // find the build directory of the declared module in the manifest
        moduleConfigs
            .forEach { module ->
                logger.debug("LOOKING for module dir {}", module.name)
                createModuleResources(module)
                module.definitionFile = definitionDirectoryPath.resolve("${module.name}.def")
                logger.debug("Setup module DONE: {}", module)
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
                // The following methods are dirty hacks for getting the implicit header needed by cinterop
                // These headers are available in the Swift Package manifest of the dependencies.
                // but it's very hard to extract them as some packages are highly customized.
                val implicitDependencies =
                    project
                        .getPackageImplicitDependencies(
                            workingDir = manifestFile.asFile.get().parentFile,
                            clonedSourcePackages = buildWorkingDir.get(),
                        ).getFolders()
                val headerSearchPaths =
                    buildList {
                        addAll(extractPublicHeaderFromCheckout(buildWorkingDir.get(), moduleConfig))
                        addAll(
                            getDirectories(
                                buildWorkingDir.get().resolve("checkouts").resolve(moduleConfig.packageName),
                                "include",
                                "public",
                                // and more?
                            ),
                        )
                        addAll(implicitDependencies)
                        addAll(findHeadersModule(clonedSourcePackages.get().resolve("artifacts"), target.get()))
                    }.distinct()
                        .joinToString(" ") { "-I\"$it\"" }
                val packageName =
                    packageDependencyPrefix.orNull?.let {
                        "$it.${moduleConfig.name}"
                    } ?: moduleConfig.name
                val compilerOpts = moduleConfig.compilerOpts.joinToString(" ")
                val linkerOps = moduleConfig.linkerOpts.joinToString(" ")
                var definition =
                    """
language = Objective-C
modules = ${moduleConfig.name}
package = $packageName
libraryPaths = "${cinteropModulePath.path}" "${productDirectory.path}"
compilerOpts = -fmodules $compilerOpts -I"${moduleMapDirectory.path}" -I"${cinteropModulePath.path}" -F"${productDirectory.path}" $headerSearchPaths
linkerOpts = -ObjC  ${if (index == 0) getLinkers() else ""} $linkerOps -F"${productDirectory.path}" ${getExtraLinkers()}
"""
                if (index == 0) {
                    val sum = productDirectory.resolve(compiledBinaryName.get()).md5()
                    definition += "\nstaticLibraries = ${compiledBinaryName.get()}"
                    definition += "\n#checksum: $sum"
                }
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

    private fun createModuleResources(module: ModuleConfig) {
        val moduleDir = cinteropModulePath.resolve(module.name + ".build")
        if (!moduleDir.exists()) {
            moduleDir.mkdirs()
        }
        val files = moduleMapDirectory.listFiles()
        files
            .firstOrNull { moduleMap ->
                moduleMap.nameWithoutExtension.lowercase() == module.name.lowercase() &&
                    moduleMap.extension == "modulemap"
            }?.toPath()
            ?.copyTo(moduleDir.resolve("module.modulemap").toPath(), overwrite = true)
        files
            .firstOrNull { moduleMap ->
                moduleMap.nameWithoutExtension == "${module.name}-Swift"
            }?.let { header ->
                header.toPath().copyTo(moduleDir.resolve(header.name).toPath(), overwrite = true)
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

    private val definitionDirectoryPath: File
        get() =
            buildWorkingDir
                .get()
                .resolve("Definitions")
                .resolve(target.get().getPackageBuildDir())

    private val productDirectory: File
        get() =
            buildWorkingDir
                .get()
                .resolve("Build")
                .resolve("Products")
                .resolve(getProductSubPath())

    private val moduleMapDirectory: File
        get() =
            buildWorkingDir
                .get()
                .resolve("Build")
                .resolve("Intermediates.noindex")
                .resolve(getMapDir())

    private fun getMapDir(): String =
        if (!target.get().isMacOS()) {
            "GeneratedModuleMaps-" + target.get().sdk()
        } else {
            "GeneratedModuleMaps"
        }

    private fun getLinkers(): String {
        val builtLibs =
            buildList {
                add("")
                //  addAll(productDirectory.listFiles { it.extension == "o" }.map { it.name })
            }.distinct().joinToString(" ") { "-l$it" }
        val frameworks =
            productDirectory
                .listFiles {
                    it.extension == "framework"
                }.map { it.nameWithoutExtension }
                .distinct()
                .joinToString(" ") { "-framework $it" }
        return "$builtLibs $frameworks"
    }

    private fun getExtraLinkers(): String {
        val xcodeDevPath = project.getXcodeDevPath()
        return buildList {
            add("-L\"$xcodeDevPath/Toolchains/XcodeDefault.xctoolchain/usr/lib/swift/${target.get().sdk()}\"")
        }.joinToString(" ")
    }

    private fun getProductSubPath(): String {
        val buildTypePrefix = if (debugMode.get()) DEBUG_PREFIX else RELEASE_PREFIX
        return if (!target.get().isMacOS()) {
            buildTypePrefix + "-" + target.get().sdk()
        } else {
            buildTypePrefix
        }
    }
}
