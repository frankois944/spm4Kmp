package fr.frankois944.spm.kmp.plugin.tasks

import fr.frankois944.spm.kmp.plugin.CompileTarget
import fr.frankois944.spm.kmp.plugin.definition.SwiftPackageDependencyDefinition
import fr.frankois944.spm.kmp.plugin.operations.getXcodeDevPath
import fr.frankois944.spm.kmp.plugin.operations.getXcodeVersion
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*
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
        @get:Input val packages: List<SwiftPackageDependencyDefinition>,
        @get:Input val debugMode: Boolean,
        @get:Input val osVersion: String,
    ) : DefaultTask() {
        init {
            description = "Generate the cinterop definitions files"
            group = "fr.frankois944.spm.kmp.plugin.tasks"
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

        private fun getBuildDirectoriesContent(): Array<File> =
            getBuildDirectory() // get folders with headers for internal dependencies
                .listFiles { it -> (it.extension == "build" || it.extension == "framework") }
                ?: emptyArray()

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
            val regex = """header\s+"([^"]+)"""".toRegex()
            return regex
                .find(module)
                ?.groupValues
                ?.map { File(it.replace("header", "").trim()) }
                ?.map { file ->
                    if (file.extension == "h") {
                        file.parentFile
                    } else {
                        file
                    }
                } ?: emptyList()
        }

        private fun getModuleNames(): List<String> =
            buildList {
                add(productName) // the first item must be the product name
                addAll(
                    packages
                        .filter {
                            it.export
                        }.flatMap {
                            it.names
                        },
                )
            }.distinct()

        private fun getExtraLinkers(): String {
            val xcodeDevPath = operation.getXcodeDevPath()

            val linkerPlatformVersion =
                if (operation.getXcodeVersion().toDouble() >= 15) {
                    target.linkerPlatformVersionName()
                } else {
                    target.linkerMinOsVersionName()
                }

            return listOf(
                "-L/usr/lib/swift",
                "-$linkerPlatformVersion",
                osVersion,
                osVersion,
                "-L$xcodeDevPath/Toolchains/XcodeDefault.xctoolchain/usr/lib/swift/${target.sdk()}",
            ).joinToString(" ")
        }

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
                            compilerOpts = -ObjC -fmodules -I"${headersPath.joinToString("\" -I\"")}"
                            linkerOpts = ${getExtraLinkers()}
                            """.trimIndent(),
                        )
                    }
                    logger.warn(
                        """
Definition File : ${moduleConfig.definitionFile.name}
At Path: ${moduleConfig.definitionFile.path}
${moduleConfig.definitionFile.readText()}
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
