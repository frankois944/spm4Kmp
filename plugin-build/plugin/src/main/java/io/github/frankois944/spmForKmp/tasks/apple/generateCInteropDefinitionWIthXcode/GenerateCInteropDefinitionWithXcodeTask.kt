package io.github.frankois944.spmForKmp.tasks.apple.generateCInteropDefinitionWIthXcode

import io.github.frankois944.spmForKmp.config.AppleCompileTarget
import io.github.frankois944.spmForKmp.config.ModuleConfig
import io.github.frankois944.spmForKmp.definition.SwiftDependency
import io.github.frankois944.spmForKmp.operations.getXcodeDevPath
import io.github.frankois944.spmForKmp.tasks.utils.TaskTracer
import io.github.frankois944.spmForKmp.tasks.utils.filterExportableDependency
import java.io.File
import javax.inject.Inject
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
import org.gradle.process.ExecOperations
import org.jetbrains.kotlin.konan.target.HostManager

@CacheableTask
@Suppress("TooManyFunctions")
internal abstract class GenerateCInteropDefinitionWithXcodeTask : DefaultTask() {
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

    @get:Input
    @get:Optional
    abstract val osVersion: Property<String>

    @get:Input
    abstract val scratchDir: Property<String>

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
                    // if (index == 0) {
                    add(
                        definitionFolder
                            .get()
                            .asFile
                            .resolve("${moduleName.name}.def"),
                    )
                    /*} else {
                        add(
                            definitionFolder
                                .get()
                                .asFile
                                .resolve("${moduleName.name}.def"),
                        )
                    }*/
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

    @get:Input
    abstract val useXcodeBuild: Property<Boolean>

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val moduleMapsDirectory: DirectoryProperty

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
                logger.warn("Product names to export: {}", it)
            }

    init {
        description = "Generate the cinterop definitions files from xcodebuild"
        group = "io.github.frankois944.spmForKmp.tasks"
        onlyIf {
            HostManager.hostIsMac
        }
    }

    @Suppress("LongMethod")
    @TaskAction
    fun generateDefinitions() {
        tracer =
            TaskTracer(
                "GenerateCInteropDefinitionWithXcodeTask-${target.get()}",
                traceEnabled.get(),
                outputFile = storedTraceFile.get().asFile,
            )
        tracer.trace("GenerateCInteropDefinitionTask") {
            tracer.trace("cleanup old definitions") {
                removeOldDefinition()
            }
        }
    }

    private fun getExtraLinkers(): String {
        val xcodeDevPath = execOps.getXcodeDevPath(logger)
        return buildList {
            add("-L\"$xcodeDevPath/Toolchains/XcodeDefault.xctoolchain/usr/lib/swift/${target.get().sdk()}\"")
        }.joinToString(" ")
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
