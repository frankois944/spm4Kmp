package io.github.frankois944.spmForKmp.tasks.apple

import io.github.frankois944.spmForKmp.operations.resolvePackage
import io.github.frankois944.spmForKmp.tasks.utils.TaskTracer
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectories
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

    @get:Input
    abstract val packageScratchDir: Property<File>

    @get:OutputFiles
    val packageScratchFiles: Map<String, File>
        get() {
            return buildMap {
                val scratchDir = packageScratchDir.get()
                val manifestParentDir = manifestFile.get().asFile.parentFile

                put(
                    "Package.resolved",
                    manifestParentDir.resolve("Package.resolved"),
                )

                val lockFile = scratchDir.resolve(".my.lock")
                put("lock", lockFile)

                val workspaceStateDir = scratchDir.resolve("my.workspace-state.json")
                put("workspace-state", workspaceStateDir)
            }
        }

    @get:OutputDirectories
    val packageScratchDirectories: Map<String, File>
        get() {
            return buildMap {
                val scratchDir = packageScratchDir.get()
                val scratchEntries =
                    listOf(
                        "artifacts" to "artifacts",
                        "checkouts" to "checkouts",
                        "registry" to "registry",
                        "repositories" to "repositories",
                    )

                for ((key, dirName) in scratchEntries) {
                    put(key, scratchDir.resolve(dirName))
                }
            }
        }

    @get:Input
    abstract val traceEnabled: Property<Boolean>

    @get:Input
    abstract val storedTracePath: Property<File>

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
                    storedTracePath
                        .get()
                        .resolve("spmForKmpTrace")
                        .resolve(
                            manifestFile
                                .get()
                                .asFile
                                .parentFile.name,
                        ).resolve("ResolveManifestTask.html"),
            )
        tracer.trace("ResolveManifestTask") {
            tracer.trace("resolvePackage") {
                try {
                    execOps.resolvePackage(
                        workingDir = manifestFile.get().asFile.parentFile,
                        scratchPath = packageScratchDir.get(),
                        sharedCachePath = sharedCacheDir.orNull,
                        sharedConfigPath = sharedConfigDir.orNull,
                        sharedSecurityPath = sharedSecurityDir.orNull,
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
        val lockFile = packageScratchDir.get().resolve(".lock")
        if (lockFile.exists()) {
            lockFile
                .copyTo(
                    packageScratchDir.get().resolve(".my.lock"),
                    true,
                )
        }
        val statFile = packageScratchDir.get().resolve("workspace-state.json")
        if (statFile.exists()) {
            statFile
                .copyTo(
                    packageScratchDir.get().resolve("my.workspace-state.json"),
                    true,
                )
        }
    }
}
