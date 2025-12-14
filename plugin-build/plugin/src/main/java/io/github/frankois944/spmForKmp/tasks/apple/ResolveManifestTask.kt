package io.github.frankois944.spmForKmp.tasks.apple

import io.github.frankois944.spmForKmp.operations.resolvePackage
import io.github.frankois944.spmForKmp.tasks.utils.TaskTracer
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectories
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.jetbrains.kotlin.konan.target.HostManager
import java.io.File
import javax.inject.Inject

@CacheableTask
internal abstract class ResolveManifestTask : DefaultTask() {
    @get:Input
    @get:Optional
    abstract val sharedCacheDir: Property<File?>

    @get:Input
    @get:Optional
    abstract val sharedConfigDir: Property<File?>

    @get:Input
    @get:Optional
    abstract val sharedSecurityDir: Property<File?>

    @get:Input
    @get:Optional
    abstract val swiftBinPath: Property<String?>

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val manifestFile: RegularFileProperty

    @get:OutputDirectories
    abstract val packageScratchDir: DirectoryProperty

    @get:Input
    abstract val traceEnabled: Property<Boolean>

    @get:Internal
    val tracer: TaskTracer by lazy {
        TaskTracer(
            "ResolveManifestTask",
            traceEnabled.get(),
            outputFile =
                project.projectDir
                    .resolve("spmForKmpTrace")
                    .resolve(
                        manifestFile
                            .get()
                            .asFile.parentFile.name,
                    ).resolve("ResolveManifestTask.html"),
        )
    }

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
        tracer.trace("ResolveManifestTask") {
            tracer.trace("resolvePackage") {
                try {
                    execOps.resolvePackage(
                        workingDir = manifestFile.get().asFile.parentFile,
                        scratchPath = packageScratchDir.get().asFile,
                        sharedCachePath = sharedCacheDir.orNull,
                        sharedConfigPath = sharedConfigDir.orNull,
                        sharedSecurityPath = sharedSecurityDir.orNull,
                        logger = logger,
                        swiftBinPath = swiftBinPath.orNull,
                    )
                } catch (ex: Exception) {
                    logger.error(
                        "Manifest file resolver :\n{}\n{}",
                        manifestFile.get(),
                        manifestFile.get().asFile.readText(),
                    )
                    throw ex
                }
            }
        }
        tracer.writeHtmlReport()
    }
}
