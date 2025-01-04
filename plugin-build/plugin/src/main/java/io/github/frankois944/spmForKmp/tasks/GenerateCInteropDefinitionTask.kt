package io.github.frankois944.spmForKmp.tasks

import io.github.frankois944.spmForKmp.CompileTarget
import io.github.frankois944.spmForKmp.definition.SwiftDependency
import io.github.frankois944.spmForKmp.operations.getXcodeDevPath
import io.github.frankois944.spmForKmp.operations.getXcodeVersion
import io.github.frankois944.spmForKmp.utils.md5
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import java.io.File
import javax.inject.Inject

private data class ModuleConfig(
    val isFramework: Boolean,
    val name: String,
    val buildDir: File,
    val definitionFile: File,
)

internal abstract class GenerateCInteropDefinitionTask : DefaultTask() {

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

    @get:Inject
    abstract val operation: ExecOperations

    private fun getBuildDirectory(): File =
        compiledBinary
            .asFile
            .get()
            .parentFile

    private fun getBuildDirectoriesContent(): List<File> =
        getBuildDirectory() // get folders with headers for internal dependencies
            .listFiles { file -> (file.extension == "build" || file.extension == "framework") }
            ?.toList()
            .orEmpty()

    private fun extractModuleNameFromModuleMap(module: String): String? {
        val regex = """module\s+(\w+)""".toRegex()
        return regex
            .find(module)
            ?.groupValues
            ?.firstOrNull()
            ?.replace("module", "")
            ?.trim()
    }

    private fun extractHeadersPathFromModuleMap(module: String): List<File> {
        /*
         * find a better regex to extract the header value
         */
        val regex = """header\s+"([^"]+)"""".toRegex()
        return regex
            .find(module)
            ?.groupValues
            ?.map {
                File(
                    it
                        .replace("header", "")
                        .replace("\"", "")
                        .trim(),
                )
            }?.map { file ->
                if (file.extension == "h") {
                    file.parentFile
                } else {
                    file
                }
            }.orEmpty()
    }

    private fun getModuleNames(): List<String> =
        buildList {
            add(productName.get()) // the first item must be the product name
            addAll(
                packages
                    .get()
                    .filter {
                        it.exportToKotlin
                    }.flatMap {
                        if (it is SwiftDependency.Package.Remote) {
                            it.names
                        } else {
                            listOf(it.packageName)
                        }
                    },
            )
        }.distinct()

    private fun getExtraLinkers(): String {
        val xcodeDevPath = operation.getXcodeDevPath(logger)

        val linkerPlatformVersion =
            @Suppress("MagicNumber")
            if (operation.getXcodeVersion(logger).toDouble() >= 15) {
                target.get().linkerPlatformVersionName()
            } else {
                target.get().linkerMinOsVersionName()
            }

        return listOf(
            "-$linkerPlatformVersion",
            osVersion.get(),
            osVersion.get(),
            "-rpath",
            "/usr/lib/swift",
            "-L\"$xcodeDevPath/Toolchains/XcodeDefault.xctoolchain/usr/lib/swift/${target.get().sdk()}\"",
        ).joinToString(" ")
    }

    @Suppress("LongMethod")
    @TaskAction
    fun generateDefinitions() {
        val moduleConfigs = mutableListOf<ModuleConfig>()
        val buildDirs = getBuildDirectoriesContent()
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
                buildDirs.find { it.nameWithoutExtension == moduleName }?.let { buildDir ->
                    logger.debug("find build dir {}", buildDir)
                    moduleConfigs.add(
                        ModuleConfig(
                            isFramework = buildDir.extension == "framework",
                            name = moduleName,
                            buildDir = buildDir,
                            definitionFile = getBuildDirectory().resolve("$moduleName.def"),
                        ),
                    )
                }
            }.also {
                logger.debug(
                    """
                    modulesConfigs found
                    $moduleConfigs
                    """.trimIndent(),
                )
            }

        moduleConfigs.forEach { moduleConfig ->
            logger.debug("Building definition file for: {}", moduleConfig)
            try {
                val libName = compiledBinary.asFile.get().name
                val checksum = compiledBinary.asFile.get().md5()
                if (moduleConfig.isFramework) {
                    val mapFile = moduleConfig.buildDir.resolve("Modules").resolve("module.modulemap")
                    logger.debug("Framework mapFile: {}", mapFile)
                    val moduleName =
                        extractModuleNameFromModuleMap(mapFile.readText())
                            ?: throw Exception("No module name from ${moduleConfig.name} in mapFile")
                    moduleConfig.definitionFile.writeText(
                        """
                        language = Objective-C
                        modules = $moduleName
                        package = ${moduleConfig.name}

                        # Set a checksum for avoid build cache
                        # checkum: $checksum
                        staticLibraries = $libName
                        libraryPaths = "${getBuildDirectory().path}"
                        compilerOpts = -fmodules -framework "${moduleConfig.buildDir.name}" -F"${getBuildDirectory().path}"
                        linkerOpts = ${getExtraLinkers()}
                        """.trimIndent(),
                    )
                } else {
                    val mapFile = moduleConfig.buildDir.resolve("module.modulemap")
                    logger.debug("Build mapFile: {}", mapFile)
                    val mapFileContent = mapFile.readText()
                    val moduleName =
                        extractModuleNameFromModuleMap(mapFileContent)
                            ?: throw RuntimeException("No module name from ${moduleConfig.name} in mapFile")
                    val headersPath = getBuildDirectoriesContent() + extractHeadersPathFromModuleMap(mapFileContent)
                    moduleConfig.definitionFile.writeText(
                        """
                        language = Objective-C
                        modules = $moduleName
                        package = ${moduleConfig.name}

                        # Set a checksum for avoid build cache
                        # checkum: $checksum
                        staticLibraries = $libName
                        libraryPaths = "${getBuildDirectory().path}"
                        compilerOpts = -ObjC -fmodules ${headersPath.joinToString(" ") { "-I\"${it.path}\"" }}
                        linkerOpts = ${getExtraLinkers()}
                        """.trimIndent(),
                    )
                }
                logger.debug(
                    """
                    Definition File : ${moduleConfig.definitionFile.name}
                    At Path: ${moduleConfig.definitionFile.path}
                    ${moduleConfig.definitionFile.readText()}}
                    """.trimIndent(),
                )
            } catch (ex: Exception) {
                logger.error(
                    """
                    Can't generate definition for ${moduleConfig.name}")
                    Expected file ${moduleConfig.definitionFile.path}
                    CONTENT ${moduleConfig.definitionFile.readText()}}
                    -> Set the `export` parameter to `false` to ignore this module
                    """.trimIndent(),
                    ex,
                )
            }
        }
    }
}
