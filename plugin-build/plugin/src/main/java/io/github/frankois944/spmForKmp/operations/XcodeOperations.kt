package io.github.frankois944.spmForKmp.operations

import io.github.frankois944.spmForKmp.CompileTarget
import io.github.frankois944.spmForKmp.dump.dependency.PackageImplicitDependencies
import io.github.frankois944.spmForKmp.dump.manifest.PackageDump
// import io.github.frankois944.spmForKmp.dependencies.PackageInternalDependencies
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
    logger: Logger? = null,
) {
    val args =
        mutableListOf(
            "swift",
            "package",
            "resolve",
            "--scratch-path",
            scratchPath.path,
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
        printExecLogs(
            logger,
            "resolvePackage",
            args,
            it.exitValue != 0,
            standardOutput,
            errorOutput,
        )
    }
}

internal fun ExecOperations.getXcodeVersion(logger: Logger? = null): String {
    val args =
        listOf(
            "xcodebuild",
            "-version",
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
        printExecLogs(
            logger,
            "getXcodeVersion",
            args,
            it.exitValue != 0,
            standardOutput,
            errorOutput,
        )
    }
    val regex = """Xcode\s(\d+\.\d+)""".toRegex()
    val match = regex.find(standardOutput.toString())
    return match?.groups?.get(1)?.value
        ?: throw RuntimeException("Can't find Xcode version with output $standardOutput")
}

internal fun ExecOperations.getXcodeDevPath(logger: Logger? = null): String {
    val args =
        listOf(
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
        printExecLogs(
            logger,
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
    target: CompileTarget,
    logger: Logger? = null,
): String {
    val args =
        listOf(
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
        printExecLogs(
            logger,
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
    logger: Logger? = null,
): PackageImplicitDependencies {
    val args =
        listOf(
            "swift",
            "package",
            "show-dependencies",
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
        printExecLogs(
            logger,
            "show-dependencies",
            args,
            it.exitValue != 0,
            standardOutput,
            errorOutput,
        )
    }
    return PackageImplicitDependencies.fromString(standardOutput.toString())
}

internal fun ExecOperations.getPackageDump(
    workingDir: File,
    scratchPath: File,
    logger: Logger? = null,
): PackageDump {
    val args =
        listOf(
            "swift",
            "package",
            "--scratch-path",
            scratchPath.path,
            "dump-package",
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
        printExecLogs(
            logger,
            "dump-package",
            args,
            it.exitValue != 0,
            standardOutput,
            errorOutput,
        )
    }
    return PackageDump.fromString(standardOutput.toString())
}

@Suppress("LongParameterList")
internal fun printExecLogs(
    logger: Logger?,
    action: String,
    args: List<String>,
    isError: Boolean,
    standardOutput: ByteArrayOutputStream,
    errorOutput: ByteArrayOutputStream,
    extraString: String? = null,
) {
    if (isError) {
        logger?.error(
            """
ERROR FOUND WHEN EXEC
RUN $action
ARGS xcrun ${args.joinToString(" ")}
ERROR $errorOutput
OUTPUT $standardOutput
###
${extraString.orEmpty()}
###
            """.trimMargin(),
        )
        throw RuntimeException(
            "RUN CMD $action failed",
        )
    } else {
        logger?.debug(
            """
RUN $action
ARGS xcrun ${args.joinToString(" ")}
OUTPUT $standardOutput
###
${extraString.orEmpty()}
###
            """.trimMargin(),
        )
    }
}
