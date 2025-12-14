package io.github.frankois944.spmForKmp.tasks.apple

import io.github.frankois944.spmForKmp.config.AppleCompileTarget
import io.github.frankois944.spmForKmp.config.ModuleConfig
import io.github.frankois944.spmForKmp.definition.SwiftDependency
import io.github.frankois944.spmForKmp.operations.getPackageImplicitDependencies
import io.github.frankois944.spmForKmp.operations.getXcodeDevPath
import io.github.frankois944.spmForKmp.tasks.utils.TaskTracer
import io.github.frankois944.spmForKmp.tasks.utils.extractModuleNameFromModuleMap
import io.github.frankois944.spmForKmp.tasks.utils.extractPublicHeaderFromCheckout
import io.github.frankois944.spmForKmp.tasks.utils.filterExportableDependency
import io.github.frankois944.spmForKmp.tasks.utils.findHeadersModule
import io.github.frankois944.spmForKmp.tasks.utils.findIncludeFolders
import io.github.frankois944.spmForKmp.tasks.utils.getModuleArtifactsPath
import io.github.frankois944.spmForKmp.tasks.utils.getModulesInBuildDirectory
import io.github.frankois944.spmForKmp.utils.checkSum
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.jetbrains.kotlin.konan.target.HostManager
import java.io.File
import javax.inject.Inject
import kotlin.io.path.exists
import kotlin.io.path.nameWithoutExtension

@CacheableTask
@Suppress("TooManyFunctions")
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

    @get:Input
    abstract val strictEnums: ListProperty<String>

    @get:Input
    abstract val nonStrictEnums: ListProperty<String>

    @get:Input
    @get:Optional
    abstract val foreignExceptionMode: Property<String?>

    @get:Input
    @get:Optional
    abstract val disableDesignatedInitializerChecks: Property<Boolean?>

    @get:Input
    @get:Optional
    abstract val userSetupHint: Property<String?>

    @get:OutputFiles
    val outputFiles: List<File>
        get() =
            buildList {
                getModuleConfigs().forEachIndexed { index, moduleName ->
                    if (index == 0) {
                        add(
                            currentBuildDirectory()
                                .resolve("${moduleName.name}_bridge.def"),
                        )
                    } else {
                        add(
                            currentBuildDirectory()
                                .resolve("${moduleName.name}.def"),
                        )
                    }
                }
            }

    @get:Input
    abstract val traceEnabled: Property<Boolean>

    @get:Inject
    abstract val execOps: ExecOperations

    @get:Internal
    val tracer: TaskTracer by lazy {
        TaskTracer(
            "GenerateCInteropDefinitionTask-${target.get()}",
            traceEnabled.get(),
            outputFile =
                project.projectDir
                    .resolve("spmForKmpTrace")
                    .resolve(scratchDir.get().parentFile.name)
                    .resolve(target.get().toString())
                    .resolve("GenerateCInteropDefinitionTask.html"),
        )
    }

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
                                        isCLang = dependency.isCLang,
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
        tracer.trace("GenerateCInteropDefinitionTask") {
            tracer.trace("cleanup old definitions") {
                removeOldDefinition()
            }

            val currentBuildDir = currentBuildDirectory()
            val compiledBinaryFile = compiledBinary.asFile.get()

            val moduleConfigs = tracer.trace("collect module configs") { getModuleConfigs() }
            val builtModules = tracer.trace("scan built modules") { getModulesInBuildDirectory(currentBuildDir) }

            tracer.trace("configure modules") {
                // Configure les répertoires et fichiers de définition pour chaque module
                configureModules(moduleConfigs, builtModules, currentBuildDir)
            }

            logger.debug(
                "Modules configured\n{}",
                moduleConfigs.joinToString("\n"),
            )

            // Génère les fichiers de définition
            tracer.trace("generate definition files") {
                moduleConfigs.forEachIndexed { index, moduleConfig ->
                    tracer.trace("build ${moduleConfig.name} definition") {
                        buildDefinitionFile(index, moduleConfig, compiledBinaryFile).let { definition ->
                            moduleConfig.definitionFile.writeText(definition.trimIndent())
                            logger.debug("Definition File : {}", moduleConfig.definitionFile.name)
                            logger.debug("At Path: {}", moduleConfig.definitionFile.path)
                            logger.debug("{}", moduleConfig.definitionFile.readText())
                        }
                    }
                }
            }
        }
        tracer.writeHtmlReport()
    }

    private fun configureModules(
        moduleConfigs: List<ModuleConfig>,
        builtModules: List<File>,
        currentBuildDir: File,
    ) {
        moduleConfigs.forEachIndexed { index, moduleConfig ->
            logger.debug("LOOKING for module dir {}", moduleConfig.name)
            if (moduleConfig.isCLang) {
                logger.warn(
                    """
                    CLang is experimental and not fully tested; please create an issue if you encounter a bug.
                    Only C language-based xcFramework is currently supported.
                    """.trimIndent(),
                )
                moduleConfig.isFramework = true
                moduleConfig.buildDir =
                    getModuleArtifactsPath(
                        fromPath = scratchDir.get().toPath(),
                        productName = productName.get(),
                        moduleConfig = moduleConfig,
                        target = target.get(),
                    )
                moduleConfig.definitionFile = currentBuildDir.resolve("${moduleConfig.name}.def")
            } else {
                builtModules
                    .find {
                        logger.debug("CHECK {} == {}", moduleConfig.name, it.nameWithoutExtension)
                        it.nameWithoutExtension.equals(moduleConfig.name, ignoreCase = true)
                    }?.let { buildDir ->
                        moduleConfig.isFramework = buildDir.extension == "framework"
                        moduleConfig.buildDir = buildDir.toPath()
                        val definitionFilePath =
                            if (index == 0) {
                                currentBuildDir.resolve("${moduleConfig.name}_bridge.def")
                            } else {
                                currentBuildDir.resolve("${moduleConfig.name}.def")
                            }
                        moduleConfig.definitionFile = definitionFilePath
                    }
            }
        }
    }

    private fun buildDefinitionFile(
        index: Int,
        moduleConfig: ModuleConfig,
        compiledBinaryFile: File,
    ): String {
        logger.debug("Building definition file for: {}", moduleConfig)
        return tracer.trace("buildDefinitionFile") {
            try {
                val moduleName =
                    tracer.trace("resolve module name") {
                        if (moduleConfig.isCLang) {
                            moduleConfig.name
                        } else {
                            val mapFile = tracer.trace("read modulemap") { getModuleMap(moduleConfig) }
                            extractModuleNameFromModuleMap(mapFile.readText())
                                ?: throw Exception("No module name for ${moduleConfig.name} in mapFile ${mapFile.path}")
                        }
                    }

                val baseDefinition =
                    tracer.trace("generate base definition") {
                        when {
                            moduleConfig.isFramework && moduleConfig.isCLang -> {
                                tracer.trace("C framework definition") { generateCFrameworkDefinition(moduleConfig) }
                            }

                            moduleConfig.isFramework -> {
                                tracer.trace("Swift framework definition") {
                                    generateFrameworkDefinition(moduleName, moduleConfig)
                                }
                            }

                            else -> {
                                tracer.trace("non-framework definition") {
                                    generateNonFrameworkDefinition(moduleName, moduleConfig)
                                }
                            }
                        }
                    }

                val definitionWithBridgeLib =
                    tracer.trace("append bridge lib and checksum (if needed)") {
                        if (index == 0) {
                            val libName = compiledBinaryFile.name
                            val checksum = tracer.trace("compute checksum") { compiledBinaryFile.checkSum() }
                            val md5 = "#checksums: $checksum ${currentBridgeHash.get()}"
                            "$baseDefinition\n$md5\nstaticLibraries = $libName"
                        } else {
                            baseDefinition
                        }
                    }

                definitionWithBridgeLib
            } catch (ex: Exception) {
                logger.error("Can't generate definition for  {}", moduleConfig.name)
                logger.error("Expected file: {}", moduleConfig.definitionFile.path)
                logger.error("Config: {}", moduleConfig)
                logger.error("Exception: $ex")
                throw GradleException(
                    "spmForKmp failed when generating definition file for ${moduleConfig.name} module",
                    ex,
                )
            }
        }
    }

    private fun getModuleMap(moduleConfig: ModuleConfig): File {
        val moduleMapPossiblePath =
            listOf(
                "module.modulemap", // non framework
                "include/module.modulemap", // non framework
                "Modules/module.modulemap", // framework
                "Modules/include/module.modulemap", // framework
            )
        logger.debug("Looking for modulemap {}", moduleMapPossiblePath)
        for (modulePath in moduleMapPossiblePath) {
            val file = moduleConfig.buildDir.resolve(modulePath)
            if (file.exists()) {
                logger.debug("modulemap found {}", path)
                return file.toFile()
            }
        }
        error("Module map file not found for module: ${moduleConfig.name}")
    }

    private fun generateCFrameworkDefinition(moduleConfig: ModuleConfig): String {
        val libraryPaths =
            getModuleArtifactsPath(
                fromPath = scratchDir.get().toPath(),
                productName = productName.get(),
                moduleConfig = moduleConfig,
                target = target.get(),
            )
        check(libraryPaths.exists())
        val packageName =
            packageDependencyPrefix.orNull?.let {
                "$it.${moduleConfig.name}"
            } ?: moduleConfig.name
        val headers =
            libraryPaths.resolve("Headers").toFile().listFiles {
                it.extension == "h"
            }
        return """
package = $packageName
headers = ${headers.joinToString { "\"$it\"" }}
headerFilter = "$libraryPaths/Headers/**"
            """.trimIndent()
    }

    private fun generateFrameworkDefinition(
        moduleName: String,
        moduleConfig: ModuleConfig,
    ): String =
        tracer.trace("generateFrameworkDefinition") {
            val frameworkName =
                tracer.trace("resolve framework name") { moduleConfig.buildDir.nameWithoutExtension }

            val packageName =
                tracer.trace("resolve package name") {
                    packageDependencyPrefix.orNull?.let {
                        "$it.${moduleConfig.name}"
                    } ?: moduleConfig.name
                }

            val buildDirPath =
                tracer.trace("resolve build dir path") { currentBuildDirectory().path }

            tracer.trace("render definition") {
                """
language = Objective-C
modules = $moduleName
package = $packageName
libraryPaths = "${currentBuildDirectory()}"
compilerOpts = -fmodules -framework "$frameworkName" -F"$buildDirPath"
linkerOpts = ${getExtraLinkers()} -framework "$frameworkName" -F"$buildDirPath"
${getCustomizedDefinitionConfig()}
                """.trimIndent()
            }
        }

    @Suppress("LongMethod")
    private fun generateNonFrameworkDefinition(
        moduleName: String,
        moduleConfig: ModuleConfig,
    ): String =
        tracer.trace("generateNonFrameworkDefinition") {
            // There are some dirty hacks for getting the headers paths needed by cinterop
            // Because, It's really difficult to extract the correct headers path from each dependency.
            // Some manifests are heavily customized and use dynamic values.
            val headerSearchPaths =
                tracer.trace("build header search paths") {
                    buildList {
                        logger.debug("SEARCH IN {}", scratchDir.get())
                        logger.debug("spmPackageName IN {}", moduleConfig.spmPackageName)

                        tracer.trace("include folders from checkout") {
                            moduleConfig.spmPackageName?.let {
                                val folderToSearch =
                                    scratchDir
                                        .get()
                                        .resolve("checkouts")
                                        .resolve(it)
                                logger.debug("SEARCH IN {}", folderToSearch)
                                // extract all folder names "include" in checkout package directory
                                addAll(findIncludeFolders(folderToSearch))
                            }
                        }

                        tracer.trace("publicHeadersPath from manifest") {
                            logger.debug("SEARCH IN extractPublicHeaderFromCheckout")
                            // extract from the current module manifest the `publicHeadersPath` values
                            addAll(extractPublicHeaderFromCheckout(scratchDir.get(), moduleConfig))
                        }

                        tracer.trace("public folders from implicit deps") {
                            logger.debug("getPackageImplicitDependencies")
                            // extract the Public third-party dependencies' for all the modules
                            tracer.trace("getPackageImplicitDependencies") {
                                val dependencies =
                                    execOps
                                        .getPackageImplicitDependencies(
                                            workingDir = manifestFile.asFile.get().parentFile,
                                            scratchPath = scratchDir.get(),
                                            logger = logger,
                                            swiftBinPath = swiftBinPath.orNull,
                                        )
                                tracer.trace("getPublicFolders") {
                                    addAll(
                                        dependencies.getPublicFolders(),
                                    )
                                }
                            }
                        }

                        tracer.trace("headers from artifacts (xcframework)") {
                            // extract the header from the SPM artifacts, which there are xcframework
                            addAll(findHeadersModule(scratchDir.get().resolve("artifacts"), target.get()))
                        }

                        // add the current build dir of the package where there are every built module
                        add(currentBuildDirectory().path)
                    }.joinToString(" ") { "-I\"$it\"" }
                }

            val packageName =
                tracer.trace("resolve package name") {
                    packageDependencyPrefix.orNull?.let {
                        "$it.${moduleConfig.name}"
                    } ?: moduleConfig.name
                }

            val compilerOpts = moduleConfig.compilerOpts.joinToString(" ")

            val linkerOps = moduleConfig.linkerOpts.joinToString(" ")

            val includeModulePath = "${moduleConfig.buildDir.resolve("include")}"

            val buildDirPath = currentBuildDirectory().path

            tracer.trace("render definition") {
                """
language = Objective-C
modules = $moduleName
package = $packageName
libraryPaths = "${currentBuildDirectory()}"
compilerOpts = $compilerOpts -fmodules -I"$includeModulePath" $headerSearchPaths -F"$buildDirPath"
linkerOpts = $linkerOps ${getExtraLinkers()} -F"$buildDirPath"
${getCustomizedDefinitionConfig()}
                """.trimIndent()
            }
        }

    private fun removeOldDefinition() {
        currentBuildDirectory().listFiles()?.forEach { file ->
            if (file.name.endsWith("_default.def") && file.exists() && file.delete()) {
                logger.debug("Removing old definition {}", file)
            }
        }
    }

    private fun getCustomizedDefinitionConfig(): String =
        buildString {
            if (strictEnums.get().isNotEmpty()) {
                appendLine("strictEnums = ${strictEnums.get().joinToString(" ")}")
            }
            if (nonStrictEnums.get().isNotEmpty()) {
                appendLine("nonStrictEnums = ${nonStrictEnums.get().joinToString(" ")}")
            }
            foreignExceptionMode.orNull?.let {
                appendLine("foreignExceptionMode = $it")
            }
            disableDesignatedInitializerChecks.orNull?.let {
                appendLine("disableDesignatedInitializerChecks = $it")
            }
            userSetupHint.orNull?.let {
                appendLine("userSetupHint = \"$it\"")
            }
        }
}
