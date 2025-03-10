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

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val bridgeSwiftSource: DirectoryProperty

    @get:Internal
    abstract val builtBridgeSwiftSource: DirectoryProperty

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

    @get:Inject
    abstract val operation: ExecOperations

    @get:Input
    abstract val xcodeBuildArgs: ListProperty<String>

    @TaskAction
    fun compilePackage() {
        logger.debug("Compile the manifest {}", manifestFile.get().path)
        prepareWorkingDir()
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
                it.workingDir = manifestFile.get().parentFile
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

    // create a empty Source Dir for xcode to resolve the package
    private fun prepareWorkingDir() {
        val destination = builtBridgeSwiftSource.get().asFile
        val source = bridgeSwiftSource.get()
        logger.warn("sourceBuildDir ${destination.path}")
        if (destination.exists()) {
            destination.deleteRecursively()
        }
        destination.mkdirs()
        if (!source.asFileTree.isEmpty
        ) {
            logger.warn(
                """
                Copy User Swift files to directory ${destination.path}
                ${source.asFileTree.files.joinToString(",")}
                """.trimIndent(),
            )
            source.asFile.copyRecursively(destination)
        } else {
            logger.warn("Copy Dummy swift file to directory {}", destination.path)
            destination.resolve("DummyFile.swift").writeText("import Foundation")
        }
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
