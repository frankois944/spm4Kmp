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
    swiftBinPath: String?,
) {
    val args =
        buildList {
            if (swiftBinPath == null) {
                add("--sdk")
                add("macosx")
                add("swift")
            }
            add("package")
            add("resolve")
            add("--scratch-path")
            add(scratchPath.path)
            add("--jobs")
            add(getNbJobs(logger))
            sharedCachePath?.let {
                add("--cache-path")
                add(it.path)
            }
            sharedConfigPath?.let {
                add("--config-path")
                add(it.path)
            }
            sharedSecurityPath?.let {
                add("--security-path")
                add(it.path)
            }
        }

    val standardOutput = ByteArrayOutputStream()
    val errorOutput = ByteArrayOutputStream()
    exec {
        it.executable = swiftBinPath ?: "xcrun"
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
    swiftBinPath: String?,
): PackageImplicitDependencies {
    val args =
        buildList {
            if (swiftBinPath == null) {
                add("--sdk")
                add("macosx")
                add("swift")
            }
            add("package")
            add("show-dependencies")
            add("--scratch-path")
            add(scratchPath.path)
            add("--format")
            add("json")
        }

    val standardOutput = ByteArrayOutputStream()
    val errorOutput = ByteArrayOutputStream()
    exec {
        it.executable = swiftBinPath ?: "xcrun"
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


internal fun ExecOperations.isDynamicLibrary(file: File, logger: Logger): Boolean {
    val args =
        listOf(
            file.path,
        )

    val standardOutput = ByteArrayOutputStream()
    val errorOutput = ByteArrayOutputStream()
    exec {
        it.executable = "file"
        it.args = args
        it.standardOutput = standardOutput
        it.errorOutput = errorOutput
        it.isIgnoreExitValue = true
    }.also {
        logger.printExecLogs(
            "isDynamicLibrary",
            args,
            it.exitValue != 0,
            standardOutput,
            errorOutput,
        )
    }
    return standardOutput.toString().contains("dynamically linked shared library")
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
