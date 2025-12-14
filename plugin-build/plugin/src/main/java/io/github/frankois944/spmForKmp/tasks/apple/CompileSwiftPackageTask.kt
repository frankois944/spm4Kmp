package io.github.frankois944.spmForKmp.tasks.apple

import io.github.frankois944.spmForKmp.config.AppleCompileTarget
import io.github.frankois944.spmForKmp.operations.getNbJobs
import io.github.frankois944.spmForKmp.operations.getSDKPath
import io.github.frankois944.spmForKmp.operations.printExecLogs
import io.github.frankois944.spmForKmp.tasks.utils.TaskTracer
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
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
    @get:Input
    abstract val target: Property<AppleCompileTarget>

    @get:Input
    abstract val debugMode: Property<Boolean>

    @get:Input
    abstract val packageScratchDir: Property<File>

    @get:Input
    @get:Optional
    abstract val osVersion: Property<String?>

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
    abstract val manifestFile: Property<File>

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val bridgeSourceDir: DirectoryProperty

    @get:OutputDirectory
    abstract val bridgeSourceBuiltDir: Property<File>

    @get:OutputDirectory
    abstract val compiledTargetDir: Property<File>

    @get:Input
    abstract val traceEnabled: Property<Boolean>

    @get:Internal
    val tracer: TaskTracer by lazy {
        TaskTracer(
            "CompileSwiftPackageTask-${target.get()}",
            traceEnabled.get(),
            outputFile =
                project.projectDir
                    .resolve("spmForKmpTrace")
                    .resolve(manifestFile.get().parentFile.name)
                    .resolve(target.get().toString())
                    .resolve("CompileSwiftPackageTask.html"),
        )
    }

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
        tracer.trace("CompileSwiftPackageTask") {
            logger.debug("Compile the manifest {}", manifestFile.get().path)
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
                    add(execOps.getSDKPath(target.get(), logger))
                    add("--triple")
                    add(target.get().triple(osVersion.orNull.orEmpty()))
                    add("--scratch-path")
                    add(packageScratchDir.get().path)
                    add("-c")
                    add(if (debugMode.get()) "debug" else "release")
                    add("--jobs")
                    add(execOps.getNbJobs(logger))
                    sharedCacheDir.orNull?.let {
                        add("--cache-path")
                        add(it.path)
                    }
                    sharedConfigDir.orNull?.let {
                        add("--config-path")
                        add(it.path)
                    }
                    sharedSecurityDir.orNull?.let {
                        add("--security-path")
                        add(it.path)
                    }
                }

            val standardOutput = ByteArrayOutputStream()
            val errorOutput = ByteArrayOutputStream()
            tracer.trace("build") {
                execOps
                    .exec {
                        it.executable = swiftBinPath.orNull ?: "xcrun"
                        it.workingDir = manifestFile.get().parentFile
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
        if (Files.isSymbolicLink(bridgeSourceBuiltDir.get().toPath())) {
            bridgeSourceBuiltDir.get().toPath().deleteIfExists()
        }
        if (bridgeSourceDir.get().asFileTree.isEmpty) {
            val dummyFile = bridgeSourceBuiltDir.get().resolve("DummySPMFile.swift")
            if (!dummyFile.exists()) {
                logger.debug("Copy Dummy swift file to directory {}", bridgeSourceBuiltDir)
                bridgeSourceBuiltDir.get().mkdirs()
                dummyFile.writeText("import Foundation")
            }
        } else {
            if (bridgeSourceBuiltDir.get().toPath().exists()) {
                logger.debug("bridgeSourceBuiltDir exist")
                if (!Files.isSymbolicLink(bridgeSourceBuiltDir.get().toPath())) {
                    logger.debug("bridgeSourceBuiltDir is not a symbolic link")
                    logger.debug("it must be deleted and be a symbolic link")
                    bridgeSourceBuiltDir.get().deleteRecursively()
                    Files.createSymbolicLink(
                        bridgeSourceBuiltDir.get().toPath(),
                        bridgeSourceDir.get().asFile.toPath(),
                    )
                }
            } else {
                logger.debug("bridgeSourceBuiltDir doesn't exist, create a symbolic Link")
                Files.createSymbolicLink(
                    bridgeSourceBuiltDir.get().toPath(),
                    bridgeSourceDir.get().asFile.toPath(),
                )
            }
        }
    }
}
