package io.github.frankois944.spmForKmp.tasks.apple

import io.github.frankois944.spmForKmp.config.AppleCompileTarget
import io.github.frankois944.spmForKmp.operations.printExecLogs
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
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
import javax.inject.Inject

@CacheableTask
internal abstract class CompileSwiftPackageTask : DefaultTask() {
    private companion object {
        const val DEBUG_PREFIX = "Debug"
        const val RELEASE_PREFIX = "Release"
    }

    init {
        description = "Compile the Swift Package manifest"
        group = "io.github.frankois944.spmForKmp.tasks.apple"
        onlyIf {
            HostManager.hostIsMac
        }
    }

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val manifestFile: Property<File>

    @get:Input
    abstract val target: Property<AppleCompileTarget>

    @get:Input
    abstract val debugMode: Property<Boolean>

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val clonedSourcePackages: DirectoryProperty

    @get:Internal
    abstract val buildWorkingDir: DirectoryProperty

    @get:Input
    @get:Optional
    abstract val packageCachePath: Property<String?>

    @get:OutputDirectory
    val productDirectory: File
        get() =
            buildWorkingDir
                .asFile
                .get()
                .resolve("Build")
                .resolve("Products")
                .resolve(getProductSubPath())

    @get:OutputDirectory
    val moduleMapDirectory: File
        get() =
            buildWorkingDir
                .asFile
                .get()
                .resolve("Build")
                .resolve("Intermediates.noindex")
                .resolve(getMapDir())

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourcePackage: Property<File>

    @get:Inject
    abstract val operation: ExecOperations

    @get:Input
    abstract val xcodeBuildArgs: ListProperty<String>

    @TaskAction
    fun compilePackage() {
        logger.debug("Compile the manifest {}", manifestFile.get().path)
        val workingDir = prepareWorkingDir()

        val args =
            buildList {
                add("xcodebuild")
                add("-scheme")
                add(manifestFile.get().parentFile.name)
                add("-derivedDataPath")
                add(buildWorkingDir.get().asFile.path)
                add("-clonedSourcePackagesDirPath")
                add(clonedSourcePackages.get().asFile.path)
                add("-configuration")
                add(if (debugMode.get()) "Debug" else "Release")
                add("-sdk")
                add(target.get().sdk())
                add("-destination")
                add("generic/platform=${target.get().destination()}")
                addAll(xcodeBuildArgs.get().orEmpty())
                packageCachePath.orNull?.let {
                    add("-packageCachePath")
                    add(it)
                }
                add("COMPILER_INDEX_STORE_ENABLE=NO")
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

    private fun getMapDir(): String =
        if (!target.get().isMacOS()) {
            "GeneratedModuleMaps-" + target.get().sdk()
        } else {
            "GeneratedModuleMaps"
        }

    private fun getProductSubPath(): String {
        val buildTypePrefix = if (debugMode.get()) DEBUG_PREFIX else RELEASE_PREFIX
        return if (!target.get().isMacOS()) {
            buildTypePrefix + "-" + target.get().sdk()
        } else {
            buildTypePrefix
        }
    }
}
