package io.github.frankois944.spmForKmp.operations

import io.github.frankois944.spmForKmp.config.AppleCompileTarget
import io.github.frankois944.spmForKmp.dump.PackageImplicitDependencies
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
        buildList {
            add("xcodebuild")
            add("-resolvePackageDependencies")
            add("-clonedSourcePackagesDirPath")
            add(clonedSourcePackages.path)
            packageCachePath?.let {
                add("-packageCachePath")
                add(it)
            }
            add("COMPILER_INDEX_STORE_ENABLE=NO")
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

internal fun Project.getPackageImplicitDependencies(
    workingDir: File,
    clonedSourcePackages: File,
): PackageImplicitDependencies {
    val operation = objects.newInstance(InjectedExecOps::class.java)
    val args =
        listOf(
            "--sdk",
            "macosx",
            "swift",
            "package",
            "show-dependencies",
            "--scratch-path",
            clonedSourcePackages.path,
            "--format",
            "json",
        )

    val standardOutput = ByteArrayOutputStream()
    val errorOutput = ByteArrayOutputStream()
    operation.execOps
        .exec {
            it.executable = "xcrun"
            it.workingDir = workingDir
            it.args = args
            it.standardOutput = standardOutput
            it.errorOutput = errorOutput
            it.isIgnoreExitValue = true
        }.also {
            printExecLogs(
                "show-dependencies",
                args,
                it.exitValue != 0,
                standardOutput,
                errorOutput,
            )
        }
    return PackageImplicitDependencies.fromString(standardOutput.toString())
}

@Suppress("LongParameterList")
internal fun Project.printExecLogs(
    action: String,
    args: List<String>,
    isError: Boolean,
    standardOutput: ByteArrayOutputStream,
    errorOutput: ByteArrayOutputStream,
    forceStandardLog: Boolean = false,
) {
    val standardLog = standardOutput.toString()

    if (isError) {
        val errorLog = errorOutput.toString()
        logger.error(
            """
ERROR FOUND WHEN EXEC
RUN $action
ARGS xcrun ${args.joinToString(" ")}
ERROR $errorLog
OUTPUT $standardLog
###
            """.trimMargin(),
        )
        if (!errorOutput.toString().contains("unexpected binary framework")) {
            throw RuntimeException(
                "RUN CMD $action failed",
            )
        }
    } else {
        if (forceStandardLog) {
            if (standardLog.isNotEmpty()) {
                logger.lifecycle(standardLog)
            }
        } else {
            logger.debug(
                """
RUN $action
ARGS xcrun ${args.joinToString(" ")}
OUTPUT $standardLog
###
                """.trimMargin(),
            )
        }
    }
}
