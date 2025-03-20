package io.github.frankois944.spmForKmp.operations

import io.github.frankois944.spmForKmp.config.AppleCompileTarget
import io.github.frankois944.spmForKmp.dump.PackageImplicitDependencies
import org.gradle.api.logging.Logger
import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream
import java.io.File

@Suppress("LongParameterList")
internal fun ExecOperations.resolvePackage(
    workingDir: File,
    scratchPath: File,
    sharedCachePath: File?,
    sharedConfigPath: File?,
    sharedSecurityPath: File?,
    logger: Logger,
) {
    val args =
        mutableListOf(
            "--sdk",
            "macosx",
            "swift",
            "package",
            "resolve",
            "--scratch-path",
            scratchPath.path,
            "--jobs",
            getNbJobs(logger),
        )
    sharedCachePath?.let {
        args.add("--cache-path")
        args.add(it.path)
    }
    sharedConfigPath?.let {
        args.add("--config-path")
        args.add(it.path)
    }
    sharedSecurityPath?.let {
        args.add("--security-path")
        args.add(it.path)
    }

    val standardOutput = ByteArrayOutputStream()
    val errorOutput = ByteArrayOutputStream()
    exec {
        it.executable = "xcrun"
        it.args = args
        it.workingDir = workingDir
        it.standardOutput = standardOutput
        it.errorOutput = errorOutput
        it.isIgnoreExitValue = true
    }.also {
        logger.printExecLogs(
            "resolvePackage",
            args,
            it.exitValue != 0,
            standardOutput,
            errorOutput,
        )
    }
}

internal fun ExecOperations.getXcodeDevPath(logger: Logger): String {
    val args =
        listOf(
            "--sdk",
            "macosx",
            "xcode-select",
            "-p",
        )

    val standardOutput = ByteArrayOutputStream()
    val errorOutput = ByteArrayOutputStream()
    exec {
        it.executable = "xcrun"
        it.args = args
        it.standardOutput = standardOutput
        it.errorOutput = errorOutput
        it.isIgnoreExitValue = true
    }.also {
        logger.printExecLogs(
            "getXcodeDevPath",
            args,
            it.exitValue != 0,
            standardOutput,
            errorOutput,
        )
    }
    return standardOutput.toString().trim()
}

internal fun ExecOperations.getSDKPath(
    target: AppleCompileTarget,
    logger: Logger,
): String {
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
    exec {
        it.executable = "xcrun"
        it.args = args
        it.standardOutput = standardOutput
        it.errorOutput = errorOutput
        it.isIgnoreExitValue = true
    }.also {
        logger.printExecLogs(
            "getSDKPath",
            args,
            it.exitValue != 0,
            standardOutput,
            errorOutput,
        )
    }
    return standardOutput.toString().trim()
}

internal fun ExecOperations.getPackageImplicitDependencies(
    workingDir: File,
    scratchPath: File,
    logger: Logger,
): PackageImplicitDependencies {
    val args =
        listOf(
            "--sdk",
            "macosx",
            "swift",
            "package",
            "show-dependencies",
            "--scratch-path",
            scratchPath.path,
            "--format",
            "json",
        )

    val standardOutput = ByteArrayOutputStream()
    val errorOutput = ByteArrayOutputStream()
    exec {
        it.executable = "xcrun"
        it.workingDir = workingDir
        it.args = args
        it.standardOutput = standardOutput
        it.errorOutput = errorOutput
        it.isIgnoreExitValue = true
    }.also {
        logger.printExecLogs(
            "show-dependencies",
            args,
            it.exitValue != 0,
            standardOutput,
            errorOutput,
        )
    }
    return PackageImplicitDependencies.fromString(standardOutput.toString())
}

internal fun ExecOperations.swiftFormat(
    file: File,
    logger: Logger,
) {
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
    exec {
        it.executable = "xcrun"
        it.args = args
        it.standardOutput = standardOutput
        it.errorOutput = errorOutput
        it.isIgnoreExitValue = true
    }.also {
        logger.printExecLogs(
            "swift-format",
            args,
            it.exitValue != 0,
            standardOutput,
            errorOutput,
        )
    }
}

internal fun ExecOperations.getNbJobs(logger: Logger): String {
    val args =
        listOf(
            "-n",
            "hw.ncpu",
        )

    val standardOutput = ByteArrayOutputStream()
    val errorOutput = ByteArrayOutputStream()
    exec {
        it.executable = "sysctl"
        it.args = args
        it.standardOutput = standardOutput
        it.errorOutput = errorOutput
        it.isIgnoreExitValue = true
    }.also {
        logger.printExecLogs(
            "getNbJobs",
            args,
            it.exitValue != 0,
            standardOutput,
            errorOutput,
        )
    }
    return standardOutput.toString().trim()
}

@Suppress("LongParameterList")
internal fun Logger.printExecLogs(
    action: String,
    args: List<String>,
    isError: Boolean,
    standardOutput: ByteArrayOutputStream,
    errorOutput: ByteArrayOutputStream,
) {
    if (isError) {
        error(
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
        debug(
            """
RUN $action
ARGS xcrun ${args.joinToString(" ")}
OUTPUT $standardOutput
###
            """.trimMargin(),
        )
    }
}
