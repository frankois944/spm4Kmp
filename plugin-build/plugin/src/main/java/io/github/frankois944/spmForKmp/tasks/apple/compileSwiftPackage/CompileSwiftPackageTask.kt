package io.github.frankois944.spmForKmp.tasks.apple.compileSwiftPackage

import io.github.frankois944.spmForKmp.config.AppleCompileTarget
import io.github.frankois944.spmForKmp.config.SpmBuildSystem
import io.github.frankois944.spmForKmp.operations.getSDKPath
import io.github.frankois944.spmForKmp.operations.printExecLogs
import io.github.frankois944.spmForKmp.operations.supportsBuildSystemFlag
import io.github.frankois944.spmForKmp.tasks.utils.TaskTracer
import io.github.frankois944.spmForKmp.tasks.utils.linkSharedResolveEntries
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectories
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.jetbrains.kotlin.konan.target.HostManager
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import javax.inject.Inject
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists

/*
 * The build directory is the only output of this task: the dependencies it needs are resolved by
 * [io.github.frankois944.spmForKmp.tasks.apple.resolveSwiftPackage.ResolveSwiftPackageTask],
 * which owns `scratch/artifacts` and `scratch/checkouts` and always runs when they are missing.
 * A cache hit therefore lands on a scratch directory where the headers referenced by the built
 * products — by absolute path — are present.
 */
@CacheableTask
internal abstract class CompileSwiftPackageTask : DefaultTask() {
    @get:Internal
    abstract val workingDir: Property<String>

    /**
     * `Package.resolved`, produced by the resolve task: a change of resolution must rebuild the
     * package. Declared as a file collection because a package whose dependencies are all local
     * or binary has nothing to pin, and SwiftPM then writes no such file.
     */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val packageResolveFile: ConfigurableFileCollection

    @get:Input
    abstract val cinteropTarget: Property<AppleCompileTarget>

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val packageSwift: RegularFileProperty

    @get:Input
    abstract val debugMode: Property<Boolean>

    /** The engine to build with, see [io.github.frankois944.spmForKmp.config.SpmBuildSystem]. */
    @get:Input
    abstract val buildSystem: Property<SpmBuildSystem>

    /**
     * The scratch directory this target is built into. Under `swiftbuild` it is one directory per
     * target, see [io.github.frankois944.spmForKmp.tasks.utils.targetScratchDirectory].
     */
    @get:Input
    abstract val packageScratchDir: Property<String>

    /**
     * The scratch directory holding the resolved dependencies, shared by every target.
     *
     * Equal to [packageScratchDir] under `native`, which resolves and builds in the same place.
     */
    @get:Input
    abstract val sharedResolveDir: Property<String>

    @get:OutputDirectories
    abstract val generatedDirs: ListProperty<File>

    @get:Input
    @get:Optional
    abstract val osVersion: Property<String>

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

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val bridgeSourceDir: DirectoryProperty

    @get:Internal
    abstract val bridgeSourceBuiltDir: DirectoryProperty

    @get:Input
    abstract val traceEnabled: Property<Boolean>

    @get:Internal
    abstract val storedTraceFile: RegularFileProperty

    @get:Input
    @get:Optional
    abstract val toolchain: Property<String>

    @get:Inject
    abstract val execOps: ExecOperations

    init {
        description = "Compile the Swift Package manifest"
        group = "io.github.frankois944.spmForKmp.tasks"
        onlyIf {
            HostManager.hostIsMac
        }
    }

    @Suppress("LongMethod")
    @TaskAction
    fun compilePackage() {
        val tracer =
            TaskTracer(
                "CompileSwiftPackageTask-${cinteropTarget.get()}",
                traceEnabled.get(),
                outputFile =
                    storedTraceFile
                        .get()
                        .asFile,
            )
        tracer.trace("CompileSwiftPackageTask") {
            tracer.trace("prepareWorkingDir") {
                prepareWorkingDir()
            }

            // Point this target's scratch directory at the shared resolution, so SwiftPM reuses
            // the checkouts instead of creating a working copy per target.
            tracer.trace("linkSharedResolveEntries") {
                linkSharedResolveEntries(
                    targetScratchDir = File(packageScratchDir.get()),
                    sharedResolveDir = File(sharedResolveDir.get()),
                    logger = logger,
                )
            }

            // SwiftPM defaults to `swiftbuild` from Swift 6.4, so the engine is always named
            // explicitly rather than left to the toolchain's default: which one built the package
            // decides how the plugin reads the scratch directory afterwards.
            val canSelectBuildSystem =
                tracer.trace("probeBuildSystemFlag") {
                    execOps.supportsBuildSystemFlag(swiftBinPath.orNull, toolchain.orNull, logger)
                }
            if (!canSelectBuildSystem && buildSystem.get() != SpmBuildSystem.NATIVE) {
                // the flag arrived with Swift 6.0, and every toolchain without it builds `native`
                logger.warn(
                    "spmForKmp: this toolchain does not support `swift build --build-system`, " +
                        "building {} with the native build system instead.",
                    cinteropTarget.get(),
                )
            }

            val args =
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
                    add("build")
                    add("-q")
                    if (canSelectBuildSystem) {
                        add("--build-system")
                        add(buildSystem.get().flagValue())
                    }
                    add("--sdk")
                    tracer.trace("getSDKPath") {
                        add(execOps.getSDKPath(cinteropTarget.get(), logger))
                    }
                    add("--triple")
                    add(cinteropTarget.get().triple(osVersion.orNull.orEmpty()))
                    add("--scratch-path")
                    add(packageScratchDir.get())
                    add("-c")
                    add(if (debugMode.get()) "debug" else "release")
                    add("--jobs")
                    add(Runtime.getRuntime().availableProcessors().toString())
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
                    add("--disable-index-store")
                    add("-debug-info-format")
                    add("none")
                }

            val standardOutput = ByteArrayOutputStream()
            val errorOutput = ByteArrayOutputStream()
            tracer.trace("build") {
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
                            "buildPackage",
                            args,
                            it.exitValue != 0,
                            standardOutput,
                            errorOutput,
                        )
                    }
            }

            tracer.trace("dereferenceSymlinks") {
                generatedDirs.get().forEach { dereferenceSymlinks(it) }
            }
        }
        tracer.writeHtmlReport()
    }

    /**
     * Replaces the symbolic links SwiftPM leaves in the build directory, in the resource bundles
     * it builds, by what they point at.
     *
     * Gradle cannot pack a symbolic link in a cache entry and fails the build when it stores one,
     * and the links SwiftPM writes are relative to the sources of the package, not to the build
     * directory, so they are usually broken where they are.
     */
    private fun dereferenceSymlinks(root: File) {
        if (!root.exists()) return
        val links =
            Files
                .walk(root.toPath())
                .use { paths ->
                    paths.filter { Files.isSymbolicLink(it) }.toList()
                }
        links.forEach { link ->
            val target = runCatching { link.toRealPath() }.getOrNull()
            val linkFile = link.toFile()
            if (target != null && Files.exists(target)) {
                logger.debug("Dereference the symbolic link {} to {}", link, target)
                linkFile.delete()
                target.toFile().copyRecursively(linkFile, overwrite = true)
            } else {
                logger.debug("Delete the broken symbolic link {}", link)
                linkFile.delete()
            }
        }
    }

    private fun prepareWorkingDir() {
        if (Files.isSymbolicLink(bridgeSourceBuiltDir.get().asFile.toPath())) {
            bridgeSourceBuiltDir
                .get()
                .asFile
                .toPath()
                .deleteIfExists()
        }
        if (bridgeSourceDir.get().asFileTree.isEmpty) {
            val dummyFile = bridgeSourceBuiltDir.get().asFile.resolve("DummySPMFile.swift")
            if (!dummyFile.exists()) {
                logger.debug("Copy Dummy swift file to directory {}", bridgeSourceBuiltDir)
                bridgeSourceBuiltDir.get().asFile.mkdirs()
                dummyFile.writeText("import Foundation")
            }
        } else {
            if (bridgeSourceBuiltDir
                    .get()
                    .asFile
                    .toPath()
                    .exists()
            ) {
                logger.debug("bridgeSourceBuiltDir exist")
                if (!Files.isSymbolicLink(bridgeSourceBuiltDir.get().asFile.toPath())) {
                    logger.debug("bridgeSourceBuiltDir is not a symbolic link")
                    logger.debug("it must be deleted and be a symbolic link")
                    bridgeSourceBuiltDir.get().asFile.deleteRecursively()
                    Files.createSymbolicLink(
                        bridgeSourceBuiltDir.get().asFile.toPath(),
                        bridgeSourceDir.get().asFile.toPath(),
                    )
                }
            } else {
                logger.debug("bridgeSourceBuiltDir doesn't exist, create a symbolic Link")
                Files.createSymbolicLink(
                    bridgeSourceBuiltDir.get().asFile.toPath(),
                    bridgeSourceDir.get().asFile.toPath(),
                )
            }
        }
    }
}
