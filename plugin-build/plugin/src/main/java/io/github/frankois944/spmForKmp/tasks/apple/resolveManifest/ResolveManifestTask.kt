package io.github.frankois944.spmForKmp.tasks.apple.resolveManifest

import io.github.frankois944.spmForKmp.operations.resolvePackage
import io.github.frankois944.spmForKmp.tasks.utils.TaskTracer
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.IgnoreEmptyDirectories
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.LocalState
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectories
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.jetbrains.kotlin.konan.target.HostManager
import java.io.File
import javax.inject.Inject

@CacheableTask
internal abstract class ResolveManifestTask : DefaultTask() {
    @get:OutputDirectory
    @get:Optional
    abstract val sharedCacheDir: DirectoryProperty

    @get:OutputDirectory
    @get:Optional
    abstract val sharedConfigDir: DirectoryProperty

    @get:OutputDirectory
    @get:Optional
    abstract val sharedSecurityDir: DirectoryProperty

    @get:Input
    @get:Optional
    abstract val swiftBinPath: Property<String>

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val manifestFile: RegularFileProperty

    @get:Input
    abstract val packageScratchPath: Property<String>

    @get:OutputFiles
    abstract val packageScratchFiles: ListProperty<File>

    @get:OutputDirectories
    abstract val packageScratchDirectories: ListProperty<File>

    @get:Input
    abstract val traceEnabled: Property<Boolean>

    @get:OutputFile
    abstract val storedTraceFile: RegularFileProperty

    @get:Inject
    abstract val execOps: ExecOperations

    init {
        description = "Resolve a Swift Package manifest"
        group = "io.github.frankois944.spmForKmp.tasks"
        onlyIf {
            HostManager.hostIsMac
        }
    }

    @TaskAction
    fun generateFile() {
        val tracer =
            TaskTracer(
                "ResolveManifestTask",
                traceEnabled.get(),
                outputFile =
                    storedTraceFile
                        .get()
                        .asFile,
            )
        tracer.trace("ResolveManifestTask") {
            tracer.trace("resolvePackage") {
                try {
                    val packageScratchDir = File(packageScratchPath.get())
                    execOps.resolvePackage(
                        workingDir = manifestFile.get().asFile.parentFile,
                        scratchPath = packageScratchDir,
                        sharedCachePath = sharedCacheDir.orNull?.asFile,
                        sharedConfigPath = sharedConfigDir.orNull?.asFile,
                        sharedSecurityPath = sharedSecurityDir.orNull?.asFile,
                        logger = logger,
                        swiftBinPath = swiftBinPath.orNull,
                    )
                    tracer.trace("copyLocalFiles") {
                        copyLocalFiles()
                    }
                } catch (ex: Exception) {
                    logger.error(
                        "Manifest file resolver :\n{}\n{}",
                        manifestFile.get(),
                        manifestFile.get().asFile.readText(),
                    )
                    throw GradleException("Failed to resolve manifest", ex)
                }
            }
        }
        tracer.writeHtmlReport()
    }

    private fun copyLocalFiles() {
        val packageScratchDir = File(packageScratchPath.get())
        val lockFile = packageScratchDir.resolve(".lock")
        if (lockFile.exists()) {
            lockFile
                .copyTo(
                    packageScratchDir.resolve(".my.lock"),
                    true,
                )
        }
        val statFile = packageScratchDir.resolve("workspace-state.json")
        if (statFile.exists()) {
            statFile
                .copyTo(
                    packageScratchDir.resolve(".my.workspace-state.json"),
                    true,
                )
        }
    }
}
