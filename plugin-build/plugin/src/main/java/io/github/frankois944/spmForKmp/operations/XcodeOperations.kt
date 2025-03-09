package io.github.frankois944.spmForKmp.operations

import io.github.frankois944.spmForKmp.config.AppleCompileTarget
import io.github.frankois944.spmForKmp.utils.InjectedExecOps
import org.gradle.api.Project
import java.io.ByteArrayOutputStream
import java.io.File

internal fun Project.resolvePackage(
    workingDir: File,
    clonedSourcePackages: File,
    packageCachePath: String?,
) {
    val operation = objects.newInstance(InjectedExecOps::class.java)
    val args =
        mutableListOf(
            "xcodebuild",
            "-resolvePackageDependencies",
            "-clonedSourcePackagesDirPath",
            clonedSourcePackages.path,
            "COMPILER_INDEX_STORE_ENABLE=NO",
        )
    packageCachePath?.let {
        args.add("-packageCachePath")
        args.add(it)
    }

    val standardOutput = ByteArrayOutputStream()
    val errorOutput = ByteArrayOutputStream()
    operation.execOps
        .exec {
            it.executable = "xcrun"
            it.args = args
            it.workingDir = workingDir
            it.standardOutput = standardOutput
            it.errorOutput = errorOutput
            it.environment("COMPILER_INDEX_STORE_ENABLE", "NO")
            it.isIgnoreExitValue = true
        }.also {
            printExecLogs(
                "resolvePackage",
                args,
                it.exitValue != 0,
                standardOutput,
                errorOutput,
            )
        }
}

internal fun Project.getXcodeDevPath(): String {
    val operation = objects.newInstance(InjectedExecOps::class.java)
    val args =
        listOf(
            "--sdk",
            "macosx",
            "xcode-select",
            "-p",
        )

    val standardOutput = ByteArrayOutputStream()
    val errorOutput = ByteArrayOutputStream()
    operation.execOps
        .exec {
            it.executable = "xcrun"
            it.args = args
            it.standardOutput = standardOutput
            it.errorOutput = errorOutput
            it.isIgnoreExitValue = true
        }.also {
            printExecLogs(
                "getXcodeDevPath",
                args,
                it.exitValue != 0,
                standardOutput,
                errorOutput,
            )
        }
    return standardOutput.toString().trim()
}

internal fun Project.getSDKPath(target: AppleCompileTarget): String {
    val operation = objects.newInstance(InjectedExecOps::class.java)
    val args =
        listOf(
            "--sdk",
            "macosx",
            "--sdk",
            target.sdk(),
            "--show-sdk-path",
        )

    val standardOutput = ByteArrayOutputStream()
    val errorOutput = ByteArrayOutputStream()
    operation.execOps
        .exec {
            it.executable = "xcrun"
            it.args = args
            it.standardOutput = standardOutput
            it.errorOutput = errorOutput
            it.isIgnoreExitValue = true
        }.also {
            printExecLogs(
                "getSDKPath",
                args,
                it.exitValue != 0,
                standardOutput,
                errorOutput,
            )
        }
    return standardOutput.toString().trim()
}

internal fun Project.swiftFormat(file: File) {
    val operation = objects.newInstance(InjectedExecOps::class.java)
    val args =
        listOf(
            "--sdk",
            "macosx",
            "swift-format",
            "-i",
            file.path,
        )

    val standardOutput = ByteArrayOutputStream()
    val errorOutput = ByteArrayOutputStream()
    operation.execOps
        .exec {
            it.executable = "xcrun"
            it.args = args
            it.standardOutput = standardOutput
            it.errorOutput = errorOutput
            it.isIgnoreExitValue = true
        }.also {
            printExecLogs(
                "swift-format",
                args,
                it.exitValue != 0,
                standardOutput,
                errorOutput,
            )
        }
}

@Suppress("LongParameterList")
internal fun Project.printExecLogs(
    action: String,
    args: List<String>,
    isError: Boolean,
    standardOutput: ByteArrayOutputStream,
    errorOutput: ByteArrayOutputStream,
) {
    if (isError) {
        logger.error(
            """
ERROR FOUND WHEN EXEC
RUN $action
ARGS xcrun ${args.joinToString(" ")}
ERROR $errorOutput
OUTPUT $standardOutput
###
            """.trimMargin(),
        )
        if (!errorOutput.toString().contains("unexpected binary framework")) {
            throw RuntimeException(
                "RUN CMD $action failed",
            )
        }
    } else {
        logger.debug(
            """
RUN $action
ARGS xcrun ${args.joinToString(" ")}
OUTPUT $standardOutput
###
            """.trimMargin(),
        )
    }
}
