package io.github.frankois944.spmForKmp.tasks.apple

import io.github.frankois944.spmForKmp.config.AppleCompileTarget
import io.github.frankois944.spmForKmp.operations.getNbJobs
import io.github.frankois944.spmForKmp.operations.getSDKPath
import io.github.frankois944.spmForKmp.operations.printExecLogs
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.jetbrains.kotlin.konan.target.HostManager
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject

@CacheableTask
internal abstract class CompileSwiftPackageTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val manifestFile: Property<File>

    @get:Input
    abstract val target: Property<AppleCompileTarget>

    @get:Input
    abstract val debugMode: Property<Boolean>

    @get:OutputDirectory
    abstract val compiledTargetDir: Property<File>

    @get:Input
    abstract val packageScratchDir: Property<File>

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val bridgeSourceDir: DirectoryProperty

    @get:OutputDirectory
    abstract val bridgeSourceBuiltDir: Property<File>

    @get:Input
    abstract val osVersion: Property<String>

    @get:Input
    @get:Optional
    abstract val sharedCacheDir: Property<File?>

    @get:Input
    @get:Optional
    abstract val sharedConfigDir: Property<File?>

    @get:Input
    @get:Optional
    abstract val sharedSecurityDir: Property<File?>

    @get:Inject
    abstract val operation: ExecOperations

    @get:Inject
    abstract val execOps: ExecOperations

    init {
        description = "Compile the Swift Package manifest"
        group = "io.github.frankois944.spmForKmp.tasks"
        bridgeSourceBuiltDir.set(
            manifestFile.map { it.parentFile.resolve("Sources") },
        )
        onlyIf {
            HostManager.hostIsMac
        }
    }

    @TaskAction
    fun compilePackage() {
        logger.debug("Compile the manifest {}", manifestFile.get().path)
        prepareWorkingDir()

        val args =
            buildList {
                add("--sdk")
                add("macosx")
                add("swift")
                add("build")
                add("--sdk")
                add(execOps.getSDKPath(target.get(), logger))
                add("--triple")
                add(target.get().triple(osVersion.get()))
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
        operation
            .exec {
                it.executable = "xcrun"
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

    private fun prepareWorkingDir() {
        if (bridgeSourceBuiltDir.get().exists()) {
            bridgeSourceBuiltDir.get().deleteRecursively()
        }
        bridgeSourceBuiltDir.get().mkdirs()
        if (!bridgeSourceDir.get().asFileTree.isEmpty) {
            logger.debug(
                """
                Copy User Swift files to directory $bridgeSourceBuiltDir
                ${bridgeSourceDir.get().asFile.list()?.toList()}
                """.trimIndent(),
            )
            bridgeSourceDir.get().asFile.copyRecursively(bridgeSourceBuiltDir.get())
        } else {
            logger.debug("Copy Dummy swift file to directory {}", bridgeSourceBuiltDir)
            bridgeSourceBuiltDir.get().resolve("DummySPMFile.swift").writeText("import Foundation")
        }
    }
}
