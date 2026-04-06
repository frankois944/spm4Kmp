package io.github.frankois944.spmForKmp.tasks.apple.generateCInteropDefinitionWIthXcode

import io.github.frankois944.spmForKmp.config.AppleCompileTarget
import io.github.frankois944.spmForKmp.definition.SwiftDependency
import io.github.frankois944.spmForKmp.tasks.utils.TaskTracer
import org.gradle.api.DefaultTask
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
import javax.inject.Inject

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
        get() = emptyList()

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

    private val checkoutFolder: File
        get() =
            File(scratchDir.get())
                .resolve("checkouts")

    private val artifactFolder: File
        get() =
            File(scratchDir.get())
                .resolve("artifacts")

    private lateinit var checkoutPublicFolder: List<File>

    private lateinit var artifactPublicFolder: List<File>

    private lateinit var builtModulesFolder: List<File>

    init {
        description = "Generate the cinterop definitions files"
        group = "io.github.frankois944.spmForKmp.tasks"
        onlyIf {
            HostManager.hostIsMac
        }
    }

    @Suppress("LongMethod")
    @TaskAction
    fun generateDefinitions() {
    }
}
