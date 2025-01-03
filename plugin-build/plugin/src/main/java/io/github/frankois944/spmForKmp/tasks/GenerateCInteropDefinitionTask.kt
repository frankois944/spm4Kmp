package io.github.frankois944.spmForKmp.tasks

import io.github.frankois944.spmForKmp.CompileTarget
import io.github.frankois944.spmForKmp.definition.SwiftDependency
import io.github.frankois944.spmForKmp.operations.getXcodeDevPath
import io.github.frankois944.spmForKmp.operations.getXcodeVersion
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
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

internal data class ModuleConfigInfo(
    val language: String,
    val modules: String,
    val `package`: String,
    val staticLibraries: String,
    val libraryPaths: String,
    val compilerOpts: String,
    val linkerOpts: String,
)

internal abstract class GenerateCInteropDefinitionTask
    @Inject
    constructor(
        @get:InputDirectory val packageBuildOutputDirectory: File,
        @get:Input val target: CompileTarget,
        @get:Input val productName: String,
        @get:Input val packages: List<SwiftDependency>,
        @get:Input val debugMode: Boolean,
        @get:Input val osVersion: String,
    ) : DefaultTask() {
        init {
            description = "Generate the cinterop definitions files"
            group = "io.github.frankois944.spmForKmp.tasks"
        }

        @get:OutputFiles
        val outputFiles: List<File>
            get() =
                buildList {
                    getModuleNames().forEach { moduleName ->
                        add(packageBuildOutputDirectory.resolve("$moduleName.def"))
                    }
                }

        @get:Inject
        abstract val operation: ExecOperations

        private fun getBuildDirectory(): File =
            packageBuildOutputDirectory
                .resolve(target.getPackageBuildDir())
                .resolve(if (debugMode) "debug" else "release")

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
                add(productName) // the first item must be the product name
                addAll(
                    packages
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
                    target.linkerPlatformVersionName()
                } else {
                    target.linkerMinOsVersionName()
                }

            return listOf(
                "-$linkerPlatformVersion",
                osVersion,
                osVersion,
                "-rpath",
                "/usr/lib/swift",
                "-L\"$xcodeDevPath/Toolchains/XcodeDefault.xctoolchain/usr/lib/swift/${target.sdk()}\"",
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
                                definitionFile = packageBuildOutputDirectory.resolve("$moduleName.def"),
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

                            staticLibraries = lib$productName.a
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

                            staticLibraries = lib$productName.a
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
                        ${moduleConfig.definitionFile.readText()}moduleConfig.definitionFile.readText()}
                        """.trimIndent(),
                    )
                } catch (ex: Exception) {
                    logger.error(
                        """
                        Can't generate definition for ${moduleConfig.name}")
                        Expected file ${moduleConfig.definitionFile.path}
                        -> Set the `export` parameter to `false` to ignore this module
                        """.trimIndent(),
                        ex,
                    )
                }
            }
        }
    }
