package fr.frankois944.spm.kmp.plugin.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject

internal abstract class CompileSwiftPackageTask : DefaultTask() {
    init {
        description = "Compile a Swift Package manifest"
        group = BasePlugin.BUILD_GROUP
    }

    @get:InputFile
    abstract val manifestFile: RegularFileProperty

    @get:Input
    abstract val target: Property<String>

    @get:Input
    abstract val debugMode: Property<Boolean>

    @get:OutputDirectory
    abstract val packageBuildOutputDirectory: DirectoryProperty

    @get:InputDirectory
    abstract val customSourcePackage: DirectoryProperty

    @get:Inject
    abstract val operation: ExecOperations

    private fun prepareWorkingDir(): File {
        val workingDir = manifestFile.asFile.get().parentFile
        val sourceDir = workingDir.resolve("Source")
        if (sourceDir.exists()) {
            sourceDir.deleteRecursively()
        }
        sourceDir.mkdirs()
        if (customSourcePackage.get().asFileTree.isEmpty) {
            sourceDir.resolve("Dummy.swift").createNewFile()
        } else {
            customSourcePackage.get().asFile.copyTo(sourceDir)
        }
        return workingDir
    }

    private fun getSDKPath(): String {
        val args =
            listOf(
                "--sdk",
                "iphonesimulator",
                "--show-sdk-path",
            )

        logger.warn(
            """
            |getSDKPath
            |Build args :
            |${args.joinToString(" ")}
            """.trimMargin(),
        )

        val output = ByteArrayOutputStream()
        operation
            .exec {
                it.executable = "xcrun"
                it.args = args
                it.standardOutput = output
            }
        return output.toString().trim()
    }

    @TaskAction
    fun compilePackage() {
        logger.warn("Compile the manifest ${manifestFile.get().asFile}")
        val sdkPath = getSDKPath()
        val workingDir = prepareWorkingDir()

        val args =
            listOf(
                "swift",
                "build",
                "--sdk",
                sdkPath,
                "--triple",
                "arm64-apple-ios16.0-simulator",
                "--scratch-path",
                packageBuildOutputDirectory.get().asFile.path,
                "-c",
                if (debugMode.get()) "debug" else "release",
            )

        logger.warn(
            """
            |compileManifest
            |Build args :
            |$workingDir
            |${args.joinToString(" ")}
            """.trimMargin(),
        )

        operation
            .exec {
                it.executable = "xcrun"
                it.workingDir = workingDir
                it.args = args
            }
    }
}
