package fr.frankois944.spm.kmp.plugin.tasks

import fr.frankois944.spm.kmp.plugin.definition.SwiftPackageDependencyDefinition
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.TaskAction
import java.io.File

private data class ModuleConfig(
    val isFramework: Boolean,
    val name: String,
    val buildDir: File,
    val definitionFile: File,
)

internal abstract class GenerateCInteropDefinitionTask : DefaultTask() {
    init {
        description = "Generate the cinterop definition"
        group = BasePlugin.BUILD_GROUP
    }

    @get:InputDirectory
    abstract val packageBuildOutputDirectory: DirectoryProperty

    @get:Input
    abstract val target: Property<String>

    @get:Input
    val productName: Property<String> = project.objects.property(String::class.java)

    @get:Input
    val packages: ListProperty<SwiftPackageDependencyDefinition> =
        project.objects.listProperty(
            SwiftPackageDependencyDefinition::class.java,
        )

    @get:OutputFiles
    val outputFiles: List<File>
        get() {
            return buildList {
                getModuleNames().forEach { moduleName ->
                    add(packageBuildOutputDirectory.asFile.get().resolve("$moduleName.def"))
                }
            }
        }

    val isDebugMode: Property<Boolean> = project.objects.property(Boolean::class.java)

    private fun getBuildDirectory(): File =
        packageBuildOutputDirectory.asFile
            .get()
            .resolve(target.get())
            .resolve(if (isDebugMode.get()) "debug" else "release")

    private fun getModuleNames(): List<String> =
        packages
            .get()
            .flatMap {
                it.names
            }.distinct()

    private fun getBuildDirectoriesContent(): Array<File> =
        getBuildDirectory()
            .listFiles() ?: emptyArray()

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
            ?.map { File(it) }
            ?.map { file ->
                if (file.extension == "h") {
                    file.parentFile
                } else {
                    file
                }
            } ?: emptyList()
    }

    @TaskAction
    fun generateDefinitions() {
        val moduleConfigs = mutableListOf<ModuleConfig>()
        val buildDirs = getBuildDirectoriesContent()
        val moduleNames = getModuleNames()

        logger.warn(
            """
            moduleNames
            $moduleNames
            """.trimIndent(),
        )

        // find the build directory of the declared module in the manifest
        moduleNames
            .forEach { moduleName ->
                buildDirs.find { it.nameWithoutExtension == moduleName }?.let { buildDir ->
                    moduleConfigs.add(
                        ModuleConfig(
                            isFramework = buildDir.extension == "framework",
                            name = moduleName,
                            buildDir = buildDir,
                            packageBuildOutputDirectory.asFile.get().resolve("$moduleName.def"),
                        ),
                    )
                }
            }.also {
                logger.warn(
                    """
                    modules
                    $moduleConfigs
                    """.trimIndent(),
                )
            }

        moduleConfigs.forEach { moduleConfig ->
            if (moduleConfig.isFramework) {
                val mapFile = moduleConfig.buildDir.resolve("Modules").resolve("module.modulemap")
                val moduleName =
                    extractModuleNameFromModuleMap(mapFile.readText())
                        ?: throw Exception("No module name from ${moduleConfig.name} in mapFile")
                moduleConfig.definitionFile.writeText(
                    """
                    language = Objective-C
                    modules = $moduleName
                    package = ${moduleConfig.name}

                    staticLibraries = lib${productName.get()}.a
                    libraryPaths = ${getBuildDirectory().path}
                    compilerOpts = -fmodules -framework -F${getBuildDirectory().path}
                    """.trimIndent(),
                )
            } else {
                val mapFile = moduleConfig.buildDir.resolve("module.modulemap")
                if (!mapFile.exists()) {
                    logger.error("Can't generate definition for ${moduleConfig.name} because no modulemap file found")
                    logger.error(
                        """
                        File: $mapFile
                        """.trimIndent(),
                    )
                } else {
                    val mapFileContent = mapFile.readText()
                    val moduleName =
                        extractModuleNameFromModuleMap(mapFileContent)
                            ?: throw Exception("No module name from ${moduleConfig.name} in mapFile")
                    val globalHeadersPath = getBuildDirectoriesContent()
                    val headersPath = globalHeadersPath + extractHeadersPathFromModuleMap(mapFileContent)
                    moduleConfig.definitionFile.writeText(
                        """
                        language = Objective-C
                        modules = $moduleName
                        package = ${moduleConfig.name}

                        staticLibraries = lib${productName.get()}.a
                        libraryPaths = ${getBuildDirectory().path}
                        compilerOpts = -ObjC -fmodules -I${headersPath.joinToString(" -I")}
                        """.trimIndent(),
                    )
                }
            }
            logger.warn(
                """
Create definition File : ${moduleConfig.definitionFile.name}
Path: ${moduleConfig.definitionFile.path}
${moduleConfig.definitionFile.readText()}
                """.trimIndent(),
            )
        }
    }
}
