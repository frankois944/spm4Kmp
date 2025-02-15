package io.github.frankois944.spmForKmp.tasks.linux

import io.github.frankois944.spmForKmp.config.LinuxCompileTarget
import io.github.frankois944.spmForKmp.config.ModuleConfig
import io.github.frankois944.spmForKmp.config.ModuleInfo
import io.github.frankois944.spmForKmp.definition.SwiftDependency
import io.github.frankois944.spmForKmp.utils.md5
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.konan.target.HostManager
import java.io.File

@CacheableTask
internal abstract class GenerateLinuxCInteropDefinitionTask : DefaultTask() {
    init {
        onlyIf {
            HostManager.hostIsLinux
        }
    }

    @get:Input
    abstract val target: Property<LinuxCompileTarget>

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
    abstract val osVersion: Property<String>

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val compiledBinary: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val manifestFile: RegularFileProperty

    @get:Input
    abstract val scratchDir: Property<File>

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val swiftSourceDir: DirectoryProperty

    @get:Input
    @get:Optional
    abstract val packageDependencyPrefix: Property<String?>

    init {
        description = "Generate the cinterop definitions files for Linux"
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

    private fun getModuleNames(): List<ModuleInfo> = listOf(
        ModuleInfo(
            name = productName.get(),
            compilerOpts = compilerOpts.get(),
            linkerOpts = linkerOpts.get(),
        )
    )


    private fun getExtraLinkers(): String {
        /*val libDir1 = File("/home/frankois/swift/swift-6.0.3/usr/lib/swift/linux").listFiles { file ->
            file.extension == "so"
        }?.map { it.nameWithoutExtension.replace("lib", "-l") }.orEmpty()
        val libDir2 = File("/home/frankois/swift/swift-6.0.3/usr/lib").listFiles { file ->
            file.extension == "so"
        }?.map { it.nameWithoutExtension.replace("lib", "-l") }.orEmpty()*/
        return buildList {
            add("-L/home/frankois/swift/swift-6.0.3/usr/lib/swift/linux/")
            add("-L/home/linuxbrew/.linuxbrew/Cellar/libobjc2/2.2.1/lib/")
            addAll(
                listOf(
                    "-lobjc",
                    "-lBlocksRuntime",
                    "-lswift_Concurrency",
                    "-lswift_StringProcessing",
                    "-lswiftDistributed",
                    "-lswiftSwiftOnoneSupport",
                    "-lswiftSynchronization",
                    "-lswiftCxx",
                    "-lFoundation",
                    "-lFoundationNetworking",
                    "-lFoundationXML",
                    "-lFoundationInternationalization",
                    "-lswiftCore",
                    "-ldispatch",
                    "-lFoundationEssentials",
                    "-lswiftDispatch",
                    "-lBlocksRuntime"
                )
            )
            add("-L/usr/lib")
            add("-lgnustep-base")
        }.joinToString(" ")
    }

    @Suppress("LongMethod")
    @TaskAction
    fun generateDefinitions() {
        val moduleName = getModuleNames().first()

        val moduleConfigs = listOf(
            ModuleConfig(
                name = moduleName.name,
                buildDir = File(""),
                packageName = moduleName.packageName,
                definitionFile = getBuildDirectory().resolve("${moduleName.name}.def"),
                linkerOpts = moduleName.linkerOpts,
                compilerOpts = moduleName.compilerOpts,
                isFramework = false
            )
        )

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

                val definition = generateDefinition(moduleConfig)
                    .let { def ->
                        // Append staticLibraries for the first index which is the bridge
                        val md5 = "#checksum: $checksum"
                        if (index == 0) "$def\n$md5\nstaticLibraries = $libName" else def
                    }
                if (definition.isNotEmpty()) {
                    moduleConfig.definitionFile.writeText(definition.trimIndent())
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

    private fun generateDefinition(
        moduleConfig: ModuleConfig,
    ): String {
        val packageName =
            packageDependencyPrefix.orNull?.let {
                "$it.${moduleConfig.name}"
            } ?: moduleConfig.name
        val compilerOpts = moduleConfig.compilerOpts.joinToString(" ")
        val linkerOps = moduleConfig.linkerOpts.joinToString(" ")
        val headers = swiftSourceDir.get().asFile.listFiles { file -> file.extension == "h" }
            ?.joinToString(" ") { it.name }
            .orEmpty()
        return """
package = $packageName
libraryPaths = "${getBuildDirectory().path}"
headers = $headers
compilerOpts = -I"${swiftSourceDir.get().asFile.absolutePath}" $compilerOpts
linkerOpts = $linkerOps ${getExtraLinkers()}
    """
    }
}
