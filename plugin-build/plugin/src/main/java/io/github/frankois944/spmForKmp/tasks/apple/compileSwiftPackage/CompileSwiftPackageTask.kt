package io.github.frankois944.spmForKmp.tasks.apple.compileSwiftPackage

import io.github.frankois944.spmForKmp.config.AppleCompileTarget
import io.github.frankois944.spmForKmp.operations.getSDKPath
import io.github.frankois944.spmForKmp.operations.printExecLogs
import io.github.frankois944.spmForKmp.tasks.utils.TaskTracer
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
import org.gradle.api.tasks.OutputDirectories
import org.gradle.api.tasks.OutputFile
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

@CacheableTask
internal abstract class CompileSwiftPackageTask : DefaultTask() {
    @get:Internal
    abstract val workingDir: Property<String>

    @get:OutputFile
    abstract val packageResolveFile: RegularFileProperty

    @get:Input
    abstract val cinteropTarget: Property<AppleCompileTarget>

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val packageSwift: RegularFileProperty

    @get:Input
    abstract val debugMode: Property<Boolean>

    @get:Input
    abstract val packageScratchDir: Property<String>

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

            val args =
                buildList {
                    if (swiftBinPath.orNull == null) {
                        add("--sdk")
                        add("macosx")
                        add("swift")
                    }
                    add("build")
                    add("-q")
                    add("--sdk")
                    tracer.trace("getSDKPath") {
                        add(execOps.getSDKPath(cinteropTarget.get(), logger))
                    }
                    add("--triple")
                    add(cinteropTarget.get().triple(osVersion.orNull.orEmpty()))
                    add("--scratch-path")
                    add(packageScratchDir.get())
                    add("--disable-sandbox")
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
        }
        tracer.writeHtmlReport()
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
