package io.github.frankois944.spmForKmp.tasks.apple.resolveSwiftPackage

import io.github.frankois944.spmForKmp.SPM_WORKSPACE_STATE_NAME
import io.github.frankois944.spmForKmp.operations.printExecLogs
import io.github.frankois944.spmForKmp.tasks.utils.TaskTracer
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.konan.target.HostManager
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject

/**
 * Resolves the dependencies of the Swift package: the source ones are checked out in
 * `scratch/checkouts`, the binary ones are downloaded and extracted in `scratch/artifacts`.
 *
 * `swift build` does it as well, but the products it builds reference those two directories by
 * absolute path — the umbrella header of a C/Objective-C dependency, the headers of an
 * xcframework — while a build cache entry only carries the build directory.
 * Resolving in its own task, which always runs when they are missing, keeps the compile task
 * cacheable: a cache hit lands on a scratch directory where the dependencies are present.
 *
 * The task is not cacheable itself: it downloads content that is already content-addressed by
 * `Package.resolved`, and a git checkout is not something to push to a build cache.
 */
@DisableCachingByDefault(because = "Downloads the dependencies, they must not be pushed to the build cache")
internal abstract class ResolveSwiftPackageTask : DefaultTask() {
    @get:Internal
    abstract val workingDir: Property<String>

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val packageSwift: RegularFileProperty

    @get:Input
    abstract val packageScratchDir: Property<String>

    @get:Input
    @get:Optional
    abstract val sharedCacheDir: Property<String>

    @get:Input
    @get:Optional
    abstract val sharedConfigDir: Property<String>

    @get:Input
    @get:Optional
    abstract val sharedSecurityDir: Property<String>

    @get:Input
    @get:Optional
    abstract val swiftBinPath: Property<String>

    @get:Input
    @get:Optional
    abstract val toolchain: Property<String>

    /**
     * `Package.resolved`. Not declared as an output: a package whose dependencies are all local
     * or binary has nothing to pin and SwiftPM writes no such file, which would keep this task
     * out of date on every build.
     */
    @get:Internal
    abstract val packageResolveFile: RegularFileProperty

    /**
     * The binary dependencies, needed by the cinterop step to find the headers of the
     * xcframeworks. Declared as an output so deleting it makes this task run again.
     */
    @get:OutputDirectory
    abstract val artifactDir: DirectoryProperty

    /**
     * The source dependencies. Not declared as an output — hashing full checkouts on every build
     * is expensive — but its absence makes the task out of date, see [expectsCheckouts].
     */
    @get:Internal
    abstract val checkoutDir: DirectoryProperty

    @get:Input
    abstract val expectsCheckouts: Property<Boolean>

    /**
     * Whether the package has a remote binary dependency, extracted in [artifactDir].
     */
    @get:Input
    abstract val expectsArtifacts: Property<Boolean>

    @get:Input
    abstract val traceEnabled: Property<Boolean>

    @get:Internal
    abstract val storedTraceFile: RegularFileProperty

    @get:Inject
    abstract val execOps: ExecOperations

    init {
        description = "Resolve the dependencies of the Swift Package"
        group = "io.github.frankois944.spmForKmp.tasks"
        onlyIf {
            HostManager.hostIsMac
        }
        outputs.upToDateWhen {
            isResolved(expectsCheckouts, checkoutDir) && isResolved(expectsArtifacts, artifactDir)
        }
    }

    @TaskAction
    fun resolvePackage() {
        val tracer =
            TaskTracer(
                "ResolveSwiftPackageTask",
                traceEnabled.get(),
                outputFile =
                    storedTraceFile
                        .get()
                        .asFile,
            )
        tracer.trace("ResolveSwiftPackageTask") {
            tracer.trace("prepareWorkspace") {
                prepareWorkspace()
            }
            val args = buildArgs()
            tracer.trace("resolve") {
                resolve(args)
            }
        }
        tracer.writeHtmlReport()
    }

    private fun isResolved(
        expected: Property<Boolean>,
        directory: DirectoryProperty,
    ): Boolean =
        !expected.get() ||
            directory
                .get()
                .asFile
                .listFiles()
                ?.isNotEmpty() == true

    /**
     * SwiftPM records what it resolved in `workspace-state.json` and trusts that record: it does
     * not fetch a dependency again while it stands, even when the files it points at are gone.
     *
     * Dropping the record when a directory the package needs is missing makes the resolution
     * below restore it, instead of leaving the cinterop step with dangling header paths.
     */
    private fun prepareWorkspace() {
        if (isResolved(expectsCheckouts, checkoutDir) && isResolved(expectsArtifacts, artifactDir)) {
            return
        }
        val state = File(packageScratchDir.get()).resolve(SPM_WORKSPACE_STATE_NAME)
        if (state.exists()) {
            logger.info("A resolved dependency is missing, delete {} to resolve it again", state)
            state.delete()
        }
    }

    private fun buildArgs(): List<String> =
        buildList {
            if (swiftBinPath.orNull == null) {
                toolchain.orNull?.let {
                    add("--toolchain")
                    add(it)
                }
                add("--sdk")
                add("macosx")
                add("swift")
            }
            add("package")
            add("--scratch-path")
            add(packageScratchDir.get())
            sharedCacheDir.orNull?.let {
                add("--cache-path")
                add(it)
            }
            sharedConfigDir.orNull?.let {
                add("--config-path")
                add(it)
            }
            sharedSecurityDir.orNull?.let {
                add("--security-path")
                add(it)
            }
            add("resolve")
        }

    private fun resolve(args: List<String>) {
        val standardOutput = ByteArrayOutputStream()
        val errorOutput = ByteArrayOutputStream()
        execOps
            .exec {
                it.executable = swiftBinPath.orNull ?: "xcrun"
                it.workingDir = File(workingDir.get())
                it.args = args
                it.standardOutput = standardOutput
                it.errorOutput = errorOutput
                it.isIgnoreExitValue = true
                toolchain.orNull?.let { toolchain ->
                    it.environment("TOOLCHAINS", toolchain)
                }
            }.also {
                logger.printExecLogs(
                    "resolvePackage",
                    args,
                    it.exitValue != 0,
                    standardOutput,
                    errorOutput,
                )
            }
    }
}
