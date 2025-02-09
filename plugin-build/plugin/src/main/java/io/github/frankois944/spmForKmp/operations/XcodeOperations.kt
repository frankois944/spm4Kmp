package io.github.frankois944.spmForKmp.operations

import io.github.frankois944.spmForKmp.config.AppleCompileTarget
import io.github.frankois944.spmForKmp.dump.PackageImplicitDependencies
import io.github.frankois944.spmForKmp.utils.InjectedExecOps
import org.gradle.api.Project
import java.io.ByteArrayOutputStream
import java.io.File

@Suppress("LongParameterList")
internal fun Project.resolvePackage(
    workingDir: File,
    scratchPath: File,
    sharedCachePath: File?,
    sharedConfigPath: File?,
    sharedSecurityPath: File?,
) {
    val operation = objects.newInstance(InjectedExecOps::class.java)
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
            getNbJobs(),
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

internal fun Project.getPackageImplicitDependencies(
    workingDir: File,
    scratchPath: File,
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
                "show-dependencies",
                args,
                it.exitValue != 0,
                standardOutput,
                errorOutput,
            )
        }
    return PackageImplicitDependencies.fromString(standardOutput.toString())
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

internal fun Project.getNbJobs(): String {
    val operation = objects.newInstance(InjectedExecOps::class.java)
    val args =
        listOf(
            "-n",
            "hw.ncpu",
        )

    val standardOutput = ByteArrayOutputStream()
    val errorOutput = ByteArrayOutputStream()
    operation.execOps
        .exec {
            it.executable = "sysctl"
            it.args = args
            it.standardOutput = standardOutput
            it.errorOutput = errorOutput
            it.isIgnoreExitValue = true
        }.also {
            printExecLogs(
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
