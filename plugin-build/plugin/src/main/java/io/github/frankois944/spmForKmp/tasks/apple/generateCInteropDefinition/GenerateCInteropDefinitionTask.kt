package io.github.frankois944.spmForKmp.tasks.apple.generateCInteropDefinition

import io.github.frankois944.spmForKmp.SWIFT_PACKAGE_NAME
import io.github.frankois944.spmForKmp.config.AppleCompileTarget
import io.github.frankois944.spmForKmp.config.ModuleConfig
import io.github.frankois944.spmForKmp.definition.SwiftDependency
import io.github.frankois944.spmForKmp.operations.getXcodeDevPath
import io.github.frankois944.spmForKmp.tasks.utils.BuiltModule
import io.github.frankois944.spmForKmp.config.SpmBuildSystem
import io.github.frankois944.spmForKmp.tasks.utils.TaskTracer
import io.github.frankois944.spmForKmp.tasks.utils.definitionLibraryPathsLine
import io.github.frankois944.spmForKmp.tasks.utils.frameworkDefinitionLinkerOpts
import io.github.frankois944.spmForKmp.tasks.utils.nonFrameworkDefinitionLinkerOpts
import io.github.frankois944.spmForKmp.tasks.utils.renderDefinition
import io.github.frankois944.spmForKmp.tasks.utils.resolveBinaryModule
import io.github.frankois944.spmForKmp.tasks.utils.spmBuildLayout
import io.github.frankois944.spmForKmp.tasks.utils.extractModuleNameFromModuleMap
import io.github.frankois944.spmForKmp.tasks.utils.filterExportableDependency
import io.github.frankois944.spmForKmp.tasks.utils.findFolders
import io.github.frankois944.spmForKmp.tasks.utils.findHeadersModule
import io.github.frankois944.spmForKmp.tasks.utils.getArtifactsDirectory
import io.github.frankois944.spmForKmp.tasks.utils.getCheckoutsDirectory
import io.github.frankois944.spmForKmp.tasks.utils.getModuleArtifactsPath
import io.github.frankois944.spmForKmp.utils.SwiftManifestParser
import io.github.frankois944.spmForKmp.utils.checkSum
import io.github.frankois944.spmForKmp.utils.findFilesRecursively
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.jetbrains.kotlin.konan.target.HostManager
import java.io.File
import java.nio.file.Path
import javax.inject.Inject
import kotlin.io.path.exists
import kotlin.io.path.nameWithoutExtension

@CacheableTask
@Suppress("TooManyFunctions")
internal abstract class GenerateCInteropDefinitionTask : DefaultTask() {
    @get:OutputDirectory
    abstract val definitionFolder: DirectoryProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val packageSwift: RegularFileProperty

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

    /**
     * When true, the generated definitions must not contain any path specific to the
     * machine running the build, so the resulting klibs can be published.
     * See `PackageRootDefinitionExtension.publishSafe`.
     */
    @get:Input
    abstract val publishSafe: Property<Boolean>

    @get:Input
    @get:Optional
    abstract val osVersion: Property<String>

    /**
     * The scratch directory holding the resolved dependencies — the checkouts and the extracted
     * xcframeworks the generated definitions point at — shared by every target.
     */
    @get:Input
    abstract val sharedScratchDir: Property<String>

    /** The scratch directory this target was built into. */
    @get:Input
    abstract val targetScratchDir: Property<String>

    @get:Input
    abstract val buildSystem: Property<SpmBuildSystem>

    @get:Input
    @get:Optional
    abstract val packageDependencyPrefix: Property<String>

    @get:Input
    @get:Optional
    abstract val swiftBinPath: Property<String>

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
    abstract val foreignExceptionMode: Property<String>

    @get:Input
    @get:Optional
    abstract val disableDesignatedInitializerChecks: Property<Boolean>

    @get:Input
    @get:Optional
    abstract val userSetupHint: Property<String>

    @get:OutputFiles
    val outputFiles: List<File>
        get() =
            buildList {
                getModuleConfigs().forEachIndexed { index, moduleName ->
                    if (index == 0) {
                        add(
                            definitionFolder
                                .get()
                                .asFile
                                .resolve("${moduleName.name}_bridge.def"),
                        )
                    } else {
                        add(
                            definitionFolder
                                .get()
                                .asFile
                                .resolve("${moduleName.name}.def"),
                        )
                    }
                }
            }

    @get:Input
    abstract val traceEnabled: Property<Boolean>

    private lateinit var tracer: TaskTracer

    @get:Internal
    abstract val storedTraceFile: RegularFileProperty

    @get:Inject
    abstract val execOps: ExecOperations

    @get:Internal
    abstract val currentBuildDirectory: DirectoryProperty

    /**
     * How to read the scratch directory the package was built into: the two build systems lay it
     * out differently, see [io.github.frankois944.spmForKmp.tasks.utils.SpmBuildLayout].
     */
    private val layout
        get() =
            spmBuildLayout(
                buildSystem = buildSystem.get(),
                packageScratchDir = File(targetScratchDir.get()),
                target = target.get(),
                buildMode = if (debugMode.get()) "debug" else "release",
            )

    private val checkoutFolder: File
        get() = getCheckoutsDirectory(File(sharedScratchDir.get()))

    private val artifactFolder: File
        get() = getArtifactsDirectory(File(sharedScratchDir.get()))

    private lateinit var checkoutPublicFolder: List<File>

    private lateinit var artifactPublicFolder: List<File>

    private lateinit var builtModulesFolder: List<File>

    /** Flags making the built modules visible to clang, see the layout. */
    private var moduleVisibilityOpts: String = ""

    init {
        description = "Generate the cinterop definitions files"
        group = "io.github.frankois944.spmForKmp.tasks"
        onlyIf {
            HostManager.hostIsMac
        }
    }

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
                                            alias = product.alias,
                                            packageName = dependency.packageName,
                                            spmPackageName = dependency.packageName,
                                        )
                                    }
                            }

                            is SwiftDependency.Binary -> {
                                listOf(
                                    ModuleConfig(
                                        name = dependency.packageName,
                                        packageName = dependency.packageName,
                                        spmPackageName = dependency.packageName,
                                        isCLang = dependency.isCLang,
                                        swiftDependency = dependency,
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

    private fun lookingForArtifactFramework(moduleConfig: ModuleConfig): File =
        artifactFolder
            .resolve(moduleConfig.packageName)
            .resolve(moduleConfig.name)
            .resolve("${moduleConfig.name}.xcframework")
            .resolve(target.get().xcFrameworkArchName())
            .resolve("Headers")
            .resolve(moduleConfig.name)

    /**
     * A binary module, read from its xcframework rather than from the build directory: only
     * `native` copies the slice there, see
     * [io.github.frankois944.spmForKmp.tasks.utils.resolveBinaryModule].
     */
    private fun resolveBinaryDependency(moduleConfig: ModuleConfig) =
        resolveBinaryModule(
            dependency = moduleConfig.swiftDependency as? SwiftDependency.Binary,
            identities =
                listOf(
                    moduleConfig.packageName,
                    moduleConfig.spmPackageName.orEmpty(),
                    productName.get().lowercase(),
                ),
            moduleName = moduleConfig.name,
            artifactsDir = artifactFolder,
            target = target.get(),
        )

    private fun getExtraLinkers(): String {
        // The toolchain path is tied to the Xcode installed on this machine; never bake it
        // into a definition that is meant to be published.
        if (publishSafe.get()) return ""
        val xcodeDevPath = execOps.getXcodeDevPath(logger)
        return buildList {
            add("-L\"$xcodeDevPath/Toolchains/XcodeDefault.xctoolchain/usr/lib/swift/${target.get().sdk()}\"")
        }.joinToString(" ")
    }

    /**
     * See [definitionLibraryPathsLine].
     */
    private fun libraryPathsLine(declaresStaticLibraries: Boolean): String? =
        definitionLibraryPathsLine(
            publishSafe = publishSafe.get(),
            declaresStaticLibraries = declaresStaticLibraries,
            buildDirectory = currentBuildDirectory.get().asFile,
        )

    @Suppress("LongMethod")
    @TaskAction
    fun generateDefinitions() {
        tracer =
            TaskTracer(
                "GenerateCInteropDefinitionTask-${target.get()}",
                traceEnabled.get(),
                outputFile = storedTraceFile.get().asFile,
            )
        tracer.trace("GenerateCInteropDefinitionTask") {
            tracer.trace("cleanup old definitions") {
                removeOldDefinition()
            }

            tracer.trace("checkoutPublicFolder") {
                checkoutPublicFolder = findFolders(checkoutFolder, "public")
            }
            tracer.trace("artifactPublicFolder") {
                artifactPublicFolder = findHeadersModule(artifactFolder, target.get())
            }
            tracer.trace("builtModulesFolder") {
                builtModulesFolder =
                    findFilesRecursively(
                        directory = currentBuildDirectory.get().asFile,
                        criteria = { file ->
                            // Early exit on non-directory to avoid string operations
                            file.isDirectory &&
                                file.extension == "build"
                        },
                        withDirectory = true,
                    )
            }

            val compiledBinaryFile = compiledBinary.asFile.get()

            val moduleConfigs = tracer.trace("collect module configs") { getModuleConfigs() }
            val builtModules =
                tracer.trace("scan built modules") {
                    layout.builtModules()
                }
            moduleVisibilityOpts =
                tracer.trace("module visibility flags") {
                    layout.moduleVisibilityCompilerOpts().joinToString(" ")
                }

            tracer.trace("configure modules") {
                // Configure les répertoires et fichiers de définition pour chaque module
                configureModules(moduleConfigs, builtModules)
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
                            try {
                                moduleConfig.definitionFile.writeText(definition.trimIndent())
                                logger.debug("Definition File : {}", moduleConfig.definitionFile.name)
                                logger.debug("At Path: {}", moduleConfig.definitionFile.path)
                                logger.debug("{}", moduleConfig.definitionFile.readText())
                            } catch (e: Exception) {
                                throw GradleException(
                                    "Error writing definition file for module ${moduleConfig.name}",
                                    e,
                                )
                            }
                        }
                    }
                }
            }
        }
        tracer.writeHtmlReport()
    }

    private fun configureModules(
        moduleConfigs: List<ModuleConfig>,
        builtModules: List<BuiltModule>,
    ) {
        moduleConfigs.forEachIndexed { index, moduleConfig ->
            logger.debug("LOOKING for module dir {}", moduleConfig.name)
            if (moduleConfig.isCLang) {
                configureCLangModule(moduleConfig)
            } else {
                configureModule(index, moduleConfig, builtModules)
            }
        }
    }

    private fun configureCLangModule(moduleConfig: ModuleConfig) {
        logger.warn(
            """
            CLang is experimental and not fully tested; please create an issue if you encounter a bug.
            Only C language-based xcFramework is currently supported.
            """.trimIndent(),
        )
        moduleConfig.isFramework = true
        moduleConfig.buildDir =
            getModuleArtifactsPath(
                fromPath = Path.of(sharedScratchDir.get()),
                productName = productName.get(),
                moduleConfig = moduleConfig,
                target = target.get(),
            )
        moduleConfig.definitionFile = definitionFolder.get().asFile.resolve("${moduleConfig.name}.def")
    }

    /**
     * A module comes from one of three places: what the build system just built, the xcframework
     * of a binary dependency, or — for a dependency neither of those resolves — the headers the
     * resolve step extracted.
     */
    private fun configureModule(
        index: Int,
        moduleConfig: ModuleConfig,
        builtModules: List<BuiltModule>,
    ) {
        val built =
            builtModules.find {
                logger.debug("CHECK {} == {}", moduleConfig.name, it.name)
                it.name.equals(moduleConfig.name, ignoreCase = true)
            }
        if (built != null) {
            moduleConfig.applyBuiltModule(built)
            moduleConfig.definitionFile = definitionFileFor(index, moduleConfig)
            return
        }
        val binary = resolveBinaryDependency(moduleConfig)
        if (binary != null) {
            logger.debug("FOUND BINARY MODULE {} in {}", binary.name, binary.buildDir)
            moduleConfig.applyBuiltModule(binary)
            moduleConfig.customSearchHeaderPath.addAll(binary.headerSearchPaths)
            moduleConfig.definitionFile = definitionFileFor(index, moduleConfig)
            return
        }
        val location = lookingForArtifactFramework(moduleConfig)
        logger.debug("FOUND ARTIFACT MODULE {}", location.name)
        if (location.exists()) {
            moduleConfig.isFramework = false
            moduleConfig.buildDir = location.toPath()
            moduleConfig.definitionFile = definitionFileFor(index, moduleConfig)
            moduleConfig.customSearchHeaderPath.add(location)
        }
    }

    private fun ModuleConfig.applyBuiltModule(built: BuiltModule) {
        isFramework = built.isFramework
        buildDir = built.buildDir.toPath()
        moduleMap = built.moduleMap
        generatedHeaderPaths = built.headerSearchPaths
    }

    /** The first module is the bridge, and its definition carries the compiled library. */
    private fun definitionFileFor(
        index: Int,
        moduleConfig: ModuleConfig,
    ): File =
        definitionFolder.get().asFile.resolve(
            if (index == 0) "${moduleConfig.name}_bridge.def" else "${moduleConfig.name}.def",
        )

    @Suppress("LongMethod")
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
                            tracer.trace("read modulemap") {
                                val mapFile = getModuleMap(moduleConfig)
                                extractModuleNameFromModuleMap(mapFile.readText())
                                    ?: throw Exception(
                                        "No module name for ${moduleConfig.name}" +
                                            " in mapFile ${mapFile.path}",
                                    )
                            }
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
                                    generateFrameworkDefinition(moduleName, moduleConfig, index == 0)
                                }
                            }

                            else -> {
                                tracer.trace("non-framework definition") {
                                    generateNonFrameworkDefinition(moduleName, moduleConfig, index == 0)
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
        moduleConfig.moduleMap?.let { moduleMap ->
            logger.debug("modulemap found {}", moduleMap)
            return moduleMap
        }
        // a module the build system did not produce: it comes from an xcframework artifact
        val moduleMapFromArtifacts = lookingForArtifactFramework(moduleConfig)
        if (moduleMapFromArtifacts.resolve("module.modulemap").exists()) {
            logger.debug("modulemap found from artifact {}", moduleMapFromArtifacts)
            moduleConfig.customSearchHeaderPath.add(moduleMapFromArtifacts)
            return moduleMapFromArtifacts.resolve("module.modulemap")
        }
        error("Module map file not found for module: ${moduleConfig.name}")
    }

    private fun generateCFrameworkDefinition(moduleConfig: ModuleConfig): String {
        val libraryPaths =
            getModuleArtifactsPath(
                fromPath = Path.of(sharedScratchDir.get()),
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
        declaresStaticLibraries: Boolean,
    ): String =
        tracer.trace("generateFrameworkDefinition") {
            val frameworkName =
                tracer.trace("resolve framework name") { moduleConfig.buildDir.nameWithoutExtension }
            val initialPackageName = moduleConfig.alias ?: moduleConfig.name
            val packageName =
                tracer.trace("resolve package name") {
                    packageDependencyPrefix.orNull?.let {
                        "$it.$initialPackageName"
                    } ?: initialPackageName
                }

            val buildDirPath =
                tracer.trace("resolve build dir path") { currentBuildDirectory.get().asFile.path }

            // a binary dependency's framework lives in its xcframework slice, not in the build
            // directory, so the directory actually holding it is named as well
            val frameworkSearchPaths =
                listOfNotNull(buildDirPath, moduleConfig.buildDir.parent?.toString())
                    .distinct()
                    .joinToString(" ") { "-F\"$it\"" }

            tracer.trace("render definition") {
                val frameworkFlag = "-framework \"$frameworkName\""
                // The search paths are local to this machine: in publishSafe mode they are kept
                // out of the klib manifest and added to this project's own link tasks instead.
                val linkerOptions =
                    frameworkDefinitionLinkerOpts(
                        publishSafe = publishSafe.get(),
                        frameworkFlag = frameworkFlag,
                        frameworkSearchPaths = frameworkSearchPaths,
                        extraLinkers = { getExtraLinkers() },
                    )
                renderDefinition(
                    listOf(
                        "language = Objective-C",
                        "modules = $moduleName",
                        "package = $packageName",
                        libraryPathsLine(declaresStaticLibraries),
                        "compilerOpts = -fmodules $moduleVisibilityOpts $frameworkFlag $frameworkSearchPaths",
                        "linkerOpts = $linkerOptions",
                        getCustomizedDefinitionConfig().trimEnd(),
                    ),
                )
            }
        }

    @Suppress("LongMethod")
    private fun generateNonFrameworkDefinition(
        moduleName: String,
        moduleConfig: ModuleConfig,
        declaresStaticLibraries: Boolean,
    ): String =
        tracer.trace("generateNonFrameworkDefinition") {
            // There are some dirty hacks for getting the headers paths needed by cinterop
            // Because, It's really difficult to extract the correct headers path from each dependency.
            // Some manifests are heavily customized and use dynamic values.
            val headerSearchPaths =
                tracer.trace("build header search paths") {
                    buildList {
                        addAll(moduleConfig.customSearchHeaderPath)
                        logger.debug("SEARCH IN {}", sharedScratchDir.get())
                        logger.debug("spmPackageName IN {}", moduleConfig.spmPackageName)
                        tracer.trace("looking for headers from checkout") {
                            moduleConfig.spmPackageName?.let { packageName ->
                                tracer.trace("looking for public folder") {
                                    logger.debug("SEARCH PUBLIC IN {}", checkoutFolder.resolve(packageName))
                                    addAll(checkoutPublicFolder)
                                }

                                tracer.trace("Looking for includes") {
                                    val productCheckoutPackage =
                                        checkoutFolder
                                            .resolve(packageName)
                                            .resolve(SWIFT_PACKAGE_NAME)
                                    logger.debug(
                                        "SEARCH include from product manifest name : {} manifest : {}",
                                        moduleConfig.name,
                                        productCheckoutPackage,
                                    )
                                    if (productCheckoutPackage.exists()) {
                                        val swiftManifestParser =
                                            SwiftManifestParser(productCheckoutPackage)
                                        swiftManifestParser.extractHeaderSearchPaths(moduleConfig.name).also {
                                            if (it.isNotEmpty()) {
                                                logger.debug("found header")
                                                it.forEach { file ->
                                                    logger.debug(file)
                                                }
                                                addAll(it)
                                            }
                                        }
                                    }
                                    addAll(builtModulesFolder)
                                }
                            }
                        }

                        tracer.trace("headers from artifacts (xcframework)") {
                            // extract the header from the SPM artifacts, which there are xcframework
                            addAll(artifactPublicFolder)
                        }

                        // add the current build dir of the package where there are every built module
                        add(currentBuildDirectory.get().asFile.path)
                    }.distinct()
                        .joinToString(" ") { "-I\"$it\"" }
                }

            val packageName =
                tracer.trace("resolve package name") {
                    packageDependencyPrefix.orNull?.let {
                        "$it.${moduleConfig.name}"
                    } ?: moduleConfig.name
                }

            val compilerOpts = moduleConfig.compilerOpts.joinToString(" ")

            val linkerOps = moduleConfig.linkerOpts.joinToString(" ")

            // `native` generates `<Module>-Swift.h` inside the module's own build directory,
            // `swiftbuild` collects them all in one GeneratedModuleMaps directory.
            val includeModulePaths =
                moduleConfig.generatedHeaderPaths
                    .ifEmpty { listOf(moduleConfig.buildDir.resolve("include").toFile()) }
                    .joinToString(" ") { "-I\"$it\"" }

            val buildDirPath = currentBuildDirectory.get().asFile.path

            tracer.trace("render definition") {
                // Only user supplied, relocatable options survive in a publishable klib.
                val linkerOptions =
                    nonFrameworkDefinitionLinkerOpts(
                        publishSafe = publishSafe.get(),
                        userLinkerOpts = linkerOps,
                        buildDirPath = buildDirPath,
                        extraLinkers = { getExtraLinkers() },
                    )
                val compilerOptions =
                    "$compilerOpts -fmodules $moduleVisibilityOpts $includeModulePaths " +
                        "$headerSearchPaths -F\"$buildDirPath\""
                renderDefinition(
                    listOf(
                        "language = Objective-C",
                        "modules = $moduleName",
                        "package = $packageName",
                        libraryPathsLine(declaresStaticLibraries),
                        "compilerOpts = $compilerOptions",
                        linkerOptions.takeIf { it.isNotBlank() }?.let { "linkerOpts = $it" },
                        getCustomizedDefinitionConfig().trimEnd(),
                    ),
                )
            }
        }

    private fun removeOldDefinition() {
        definitionFolder.get().asFileTree.forEach { file ->
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
