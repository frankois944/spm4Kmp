package io.github.frankois944.spmForKmp.tasks.linux

import io.github.frankois944.spmForKmp.config.LinuxCompileTarget
import io.github.frankois944.spmForKmp.operations.printExecLogs
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.process.ExecOperations
import org.jetbrains.kotlin.konan.target.HostManager
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject

@CacheableTask
internal abstract class CompileLinuxSwiftPackageTask : DefaultTask() {

    init {
        onlyIf {
            HostManager.hostIsLinux
        }
    }

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val manifestFile: Property<File>

    @get:Input
    abstract val target: Property<LinuxCompileTarget>

    @get:Input
    abstract val debugMode: Property<Boolean>

    @get:OutputDirectory
    abstract val compiledTargetDir: Property<File>

    @get:Input
    abstract val packageScratchDir: Property<File>

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
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
        description = "Compile the Swift Package manifest for Linux"
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
                ${sourcePackage.get().list()?.toList()}
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
        val workingDir = prepareWorkingDir()

        val args = mutableListOf(
            "build",
            "--scratch-path",
            packageScratchDir.get().path,
            "-c",
            if (debugMode.get()) "debug" else "release",
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
                it.executable = "swift"
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
