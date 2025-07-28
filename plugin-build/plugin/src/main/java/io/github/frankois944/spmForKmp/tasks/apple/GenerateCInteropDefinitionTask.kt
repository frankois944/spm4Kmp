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
import io.github.frankois944.spmForKmp.tasks.utils.findIncludeFolders
import io.github.frankois944.spmForKmp.tasks.utils.getModulesInBuildDirectory
import io.github.frankois944.spmForKmp.utils.checkSum
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
import org.gradle.process.ExecOperations
import org.jetbrains.kotlin.konan.target.HostManager
import java.io.File
import javax.inject.Inject
import kotlin.math.log

@CacheableTask
internal abstract class GenerateCInteropDefinitionTask : DefaultTask() {
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
    @get:Optional
    abstract val osVersion: Property<String?>

    @get:Input
    abstract val scratchDir: Property<File>

    @get:Input
    @get:Optional
    abstract val packageDependencyPrefix: Property<String?>

    @get:Input
    @get:Optional
    abstract val swiftBinPath: Property<String?>

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val compiledBinary: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val manifestFile: RegularFileProperty

    @get:Input
    abstract val currentBridgeHash: Property<String>

    @get:OutputFiles
    val outputFiles: List<File>
        get() =
            buildList {
                getModuleConfigs().forEachIndexed { index, moduleName ->
                    if (index == 0) {
                        add(
                            currentBuildDirectory()
                                .resolve("${moduleName.name}_${currentBridgeHash.get()}_default.def"),
                        )
                    } else {
                        add(
                            currentBuildDirectory()
                                .resolve("${moduleName.name}.def"),
                        )
                    }
                }
            }

    @get:Inject
    abstract val execOps: ExecOperations

    init {
        description = "Generate the cinterop definitions files"
        group = "io.github.frankois944.spmForKmp.tasks"
        onlyIf {
            HostManager.hostIsMac
        }
    }

    private fun currentBuildDirectory(): File =
        compiledBinary
            .asFile
            .get()
            .parentFile

    private fun getModuleConfigs(): List<ModuleConfig> =
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
                                    ),
                                )
                            }
                        }
                    },
            )
        }.distinctBy { it.name }
            .also {
                logger.debug("Product names to export: {}", it)
            }

    private fun getExtraLinkers(): String {
        val xcodeDevPath = execOps.getXcodeDevPath(logger)
        return buildList {
            add("-L\"$xcodeDevPath/Toolchains/XcodeDefault.xctoolchain/usr/lib/swift/${target.get().sdk()}\"")
        }.joinToString(" ")
    }

    @Suppress("LongMethod")
    @TaskAction
    fun generateDefinitions() {
        removeOldDefinition()
        val moduleConfigs = getModuleConfigs()
        val buildDirContent = getModulesInBuildDirectory(currentBuildDirectory())
        // find the build directory of the declared module in the manifest
        moduleConfigs
            .forEachIndexed { index, moduleInfo ->
                logger.debug("LOOKING for module dir {}", moduleInfo.name)
                buildDirContent
                    .find {
                        logger.debug("CHECK {} == {}", moduleInfo.name, it.nameWithoutExtension)
                        it.nameWithoutExtension.lowercase() == moduleInfo.name.lowercase()
                    }?.let { buildDir ->
                        moduleInfo.isFramework = buildDir.extension == "framework"
                        moduleInfo.buildDir = buildDir
                        moduleInfo.definitionFile =
                            if (index == 0) {
                                currentBuildDirectory()
                                    .resolve("${moduleInfo.name}_${currentBridgeHash.get()}_default.def")
                            } else {
                                currentBuildDirectory()
                                    .resolve("${moduleInfo.name}.def")
                            }
                    }
            }
        logger.debug(
            "Modules configured\n{}",
            moduleConfigs.joinToString("\n"),
        )
        moduleConfigs.forEachIndexed { index, moduleConfig ->
            logger.debug("Building definition file for: {}", moduleConfig)
            try {
                val mapFile =
                    moduleConfig.buildDir.resolve(
                        if (moduleConfig.isFramework) "Modules/module.modulemap" else "module.modulemap",
                    )
                val moduleName =
                    extractModuleNameFromModuleMap(mapFile.readText())
                        ?: throw Exception("No module name for ${moduleConfig.name} in mapFile ${mapFile.path}")
                val definition =
                    if (moduleConfig.isFramework) {
                        generateFrameworkDefinition(moduleName, moduleConfig)
                    } else {
                        generateNonFrameworkDefinition(moduleName, moduleConfig)
                    }.let { def ->
                        // Append staticLibraries for the first index which is the bridge
                        if (index == 0) {
                            val libName = compiledBinary.asFile.get().name
                            val checksum = compiledBinary.asFile.get().checkSum()
                            val md5 = "#checksum: $checksum"
                            "$def\n$md5\nstaticLibraries = $libName"
                        } else {
                            def
                        }
                    }
                moduleConfig.definitionFile.writeText(definition.trimIndent())
                logger.debug("######")
                logger.debug("Definition File : {}", moduleConfig.definitionFile.name)
                logger.debug("At Path: {}", moduleConfig.definitionFile.path)
                logger.debug("{}", moduleConfig.definitionFile.readText())
                logger.debug("######")
            } catch (ex: Exception) {
                logger.error("######")
                logger.error("Can't generate definition for  {}", moduleConfig.name)
                logger.error("Expected file: {}", moduleConfig.definitionFile.path)
                logger.error("Config: {}", moduleConfig)
                logger.error("######", ex)
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
            libraryPaths = "${currentBuildDirectory().path}"
            compilerOpts = -fmodules -framework "$frameworkName" -F"${currentBuildDirectory().path}"
            linkerOpts = ${getExtraLinkers()} -framework "$frameworkName" -F"${currentBuildDirectory().path}"
            """.trimIndent()
    }

    private fun generateNonFrameworkDefinition(
        moduleName: String,
        moduleConfig: ModuleConfig,
    ): String {
        // There are some dirty hacks for getting the headers paths needed by cinterop
        // Because, It's really difficult to extract the correct headers path from each dependency.
        // Some manifests are heavily customized and use dynamic values.
        val headerSearchPaths =
            buildList {
                logger.debug("SEARCH IN {}", scratchDir.get())
                logger.debug("spmPackageName IN {}", moduleConfig.spmPackageName)
                moduleConfig.spmPackageName?.let {
                    val folderToSearch =
                        scratchDir
                            .get()
                            .resolve("checkouts")
                            .resolve(it)
                            .resolve("Sources")
                    logger.debug("SEARCH IN {}", folderToSearch)
                    // extract all folder names "include" in checkout package directory
                    addAll(
                        findIncludeFolders(folderToSearch),
                    )
                }
                logger.debug("SEARCH IN extractPublicHeaderFromCheckout")
                // extract from the current module manifest the `publicHeadersPath` values
                addAll(extractPublicHeaderFromCheckout(scratchDir.get(), moduleConfig))
                // extract the Public third-party dependencies' for all the modules
                logger.debug("getPackageImplicitDependencies")
                addAll(
                    execOps
                        .getPackageImplicitDependencies(
                            workingDir = manifestFile.asFile.get().parentFile,
                            scratchPath = scratchDir.get(),
                            logger = logger,
                            swiftBinPath = swiftBinPath.orNull,
                        ).getPublicFolders(),
                )
                // extract the header from the SPM artifacts, which there are xcframework
                addAll(findHeadersModule(scratchDir.get().resolve("artifacts"), target.get()))
                // add the current build dir of the package where there are every built module
                add(currentBuildDirectory().path)
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
            libraryPaths = "${currentBuildDirectory().path}"
            compilerOpts = $compilerOpts -fmodules $headerSearchPaths -F"${currentBuildDirectory().path}"
            linkerOpts = $linkerOps ${getExtraLinkers()} -F"${currentBuildDirectory().path}"
            """.trimIndent()
    }

    private fun removeOldDefinition() {
        currentBuildDirectory().listFiles()?.forEach { file ->
            if (file.name.endsWith("_default.def") && file.exists() && file.delete()) {
                logger.debug("Removing old definition {}", file)
            }
        }
    }
}
