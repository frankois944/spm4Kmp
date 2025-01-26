package io.github.frankois944.spmForKmp.tasks

import io.github.frankois944.spmForKmp.config.CompileTarget
import io.github.frankois944.spmForKmp.operations.getNbJobs
import io.github.frankois944.spmForKmp.operations.getSDKPath
import io.github.frankois944.spmForKmp.operations.printExecLogs
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.jetbrains.kotlin.konan.target.HostManager
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject

internal abstract class CompileSwiftPackageTask : DefaultTask() {
    init {
        onlyIf {
            HostManager.hostIsMac
        }
    }

    @get:InputFile
    abstract val manifestFile: Property<File>

    @get:Input
    abstract val target: Property<CompileTarget>

    @get:Input
    abstract val debugMode: Property<Boolean>

    @get:OutputDirectory
    abstract val compiledTargetDir: Property<File>

    @get:Input
    abstract val packageScratchDir: Property<File>

    @get:InputDirectory
    abstract val sourcePackage: Property<File>

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

    init {
        description = "Compile the Swift Package manifest"
        group = "io.github.frankois944.spmForKmp.tasks"
    }

    private fun prepareWorkingDir(): File {
        val workingDir = manifestFile.get().parentFile
        val sourceDir = workingDir.resolve("Sources")
        if (sourceDir.exists()) {
            sourceDir.deleteRecursively()
        }
        sourceDir.mkdirs()
        if (sourcePackage.get().list()?.isNotEmpty() == true) {
            logger.debug(
                """
                Copy User Swift files to directory $sourceDir
                ${sourcePackage.get().list()?.toList()}sourcePackage.get().list()?.toList()}
                """.trimIndent(),
            )
            sourcePackage.get().copyRecursively(sourceDir)
        } else {
            logger.debug("Copy Dummy swift file to directory {}", sourceDir)
            sourceDir.resolve("DummySPMFile.swift").writeText("import Foundation")
        }
        return workingDir
    }

    @TaskAction
    fun compilePackage() {
        logger.debug("Compile the manifest {}", manifestFile.get().path)
        val sdkPath = project.getSDKPath(target.get())
        val workingDir = prepareWorkingDir()

        val args =
            mutableListOf(
                "--sdk",
                "macosx",
                "swift",
                "build",
                "--sdk",
                sdkPath,
                "--triple",
                target.get().getTriple(osVersion.get()),
                "--scratch-path",
                packageScratchDir.get().path,
                "-c",
                if (debugMode.get()) "debug" else "release",
                "--jobs",
                project.getNbJobs(),
            )
        sharedCacheDir.orNull?.let {
            args.add("--cache-path")
            args.add(it.path)
        }
        sharedConfigDir.orNull?.let {
            args.add("--config-path")
            args.add(it.path)
        }
        sharedSecurityDir.orNull?.let {
            args.add("--security-path")
            args.add(it.path)
        }

        val standardOutput = ByteArrayOutputStream()
        val errorOutput = ByteArrayOutputStream()
        operation
            .exec {
                it.executable = "xcrun"
                it.workingDir = workingDir
                it.args = args
                it.standardOutput = standardOutput
                it.errorOutput = errorOutput
                it.isIgnoreExitValue = true
            }.also {
                project.printExecLogs(
                    "buildPackage",
                    args,
                    it.exitValue != 0,
                    standardOutput,
                    errorOutput,
                )
            }
    }
}
