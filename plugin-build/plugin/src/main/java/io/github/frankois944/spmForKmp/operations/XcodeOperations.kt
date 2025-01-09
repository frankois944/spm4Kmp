package io.github.frankois944.spmForKmp.operations

import io.github.frankois944.spmForKmp.CompileTarget
import io.github.frankois944.spmForKmp.dump.dependency.PackageImplicitDependencies
import io.github.frankois944.spmForKmp.utils.InjectedExecOps
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import java.io.ByteArrayOutputStream
import java.io.File

@Suppress("LongParameterList")
internal fun Project.resolvePackage(
    workingDir: File,
    scratchPath: File,
    sharedCachePath: File?,
    sharedConfigPath: File?,
    sharedSecurityPath: File?,
    logger: Logger? = null,
) {
    val operation = objects.newInstance(InjectedExecOps::class.java)
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
                logger,
                "resolvePackage",
                args,
                it.exitValue != 0,
                standardOutput,
                errorOutput,
            )
        }
}

internal fun Project.getXcodeVersion(logger: Logger? = null): String {
    val operation = objects.newInstance(InjectedExecOps::class.java)
    val args =
        listOf(
            "xcodebuild",
            "-version",
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

internal fun Project.getXcodeDevPath(logger: Logger? = null): String {
    val operation = objects.newInstance(InjectedExecOps::class.java)
    val args =
        listOf(
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

internal fun Project.getSDKPath(
    target: CompileTarget,
    logger: Logger? = null,
): String {
    val operation = objects.newInstance(InjectedExecOps::class.java)
    val args =
        listOf(
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

internal fun Project.getPackageImplicitDependencies(
    workingDir: File,
    scratchPath: File,
    logger: Logger? = null,
): PackageImplicitDependencies {
    val operation = objects.newInstance(InjectedExecOps::class.java)
    val args =
        listOf(
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
