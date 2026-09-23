package io.github.frankois944.spmForKmp.operations

import io.github.frankois944.spmForKmp.config.AppleCompileTarget
import org.gradle.api.GradleException
import org.gradle.api.logging.Logger
import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.ConcurrentHashMap

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

internal fun ExecOperations.isDynamicLibrary(
    file: File,
    logger: Logger,
): Boolean {
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

internal fun ExecOperations.makeDirectoryWritable(
    file: File,
    logger: Logger,
) {
    val args =
        listOf(
            "-R",
            "+w",
            file.path,
        )
    val standardOutput = ByteArrayOutputStream()
    val errorOutput = ByteArrayOutputStream()
    exec {
        it.executable = "chmod"
        it.args = args
        it.standardOutput = standardOutput
        it.errorOutput = errorOutput
        it.isIgnoreExitValue = true
    }.also {
        logger.printExecLogs(
            "makeDirectoryWritable",
            args,
            it.exitValue != 0,
            standardOutput,
            errorOutput,
        )
    }
}

internal fun ExecOperations.signFramework(
    file: File,
    signIdentityName: String,
    logger: Logger,
) {
    val args =
        listOf(
            "--sdk",
            "macosx",
            "codesign",
            "--force",
            "--sign",
            signIdentityName,
            "--preserve-metadata=identifier,entitlements",
            "--timestamp=none",
            file.absolutePath,
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
            "signFramework",
            args,
            it.exitValue != 0,
            standardOutput,
            errorOutput,
        )
    }
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
        val errorString =
            """
ERROR FOUND WHEN EXEC
RUN $action
ARGS xcrun ${args.joinToString(" ")}
ERROR $errorOutput
OUTPUT $standardOutput
###
            """.trimMargin()
        if (!errorOutput.toString().contains("unexpected binary")) {
            errorString.lines().forEach { errorLine ->
                error(errorLine)
            }
            throw GradleException(
                "spmForKmp failed when running $action",
                Exception("ERROR FOUND WHEN EXEC $action"),
            )
        } else {
            errorString.lines().forEach { errorLine ->
                warn(errorLine)
            }
        }
    } else {
        debug("RUN $action")
        debug("ARGS xcrun ${args.joinToString(" ")}")
        debug("OUTPUT $standardOutput")
    }
}

/**
 * Whether the toolchain understands `swift build --build-system <name>`.
 *
 * The flag only exists from Swift 6.0 onwards; the plugin still supports older toolchains through
 * `swiftBinPath`/`toolchain`, so the capability is probed instead of being derived from a version.
 * `native` is a valid value wherever the flag exists (Swift 6.0 offered `native`/`xcode`, later
 * releases `native`/`swiftbuild`), so detecting the flag is enough to know the value is accepted.
 *
 * The result is memoized per toolchain: the probe is cheap but runs once per compile task.
 */
private val buildSystemFlagSupport = ConcurrentHashMap<String, Boolean>()

internal fun ExecOperations.supportsBuildSystemFlag(
    swiftBinPath: String?,
    toolchain: String?,
    logger: Logger,
): Boolean =
    buildSystemFlagSupport.computeIfAbsent("${swiftBinPath.orEmpty()}|${toolchain.orEmpty()}") { _ ->
        val args =
            buildList {
                if (swiftBinPath == null) {
                    toolchain?.let {
                        add("--toolchain")
                        add(it)
                    }
                    add("--sdk")
                    add("macosx")
                    add("swift")
                }
                add("build")
                add("--help")
            }

        val standardOutput = ByteArrayOutputStream()
        val errorOutput = ByteArrayOutputStream()
        val result =
            exec {
                it.executable = swiftBinPath ?: "xcrun"
                it.args = args
                it.standardOutput = standardOutput
                it.errorOutput = errorOutput
                it.isIgnoreExitValue = true
                toolchain?.let { value ->
                    it.environment("TOOLCHAINS", value)
                }
            }

        if (result.exitValue != 0) {
            logger.info("Could not probe `swift build --help`, assuming --build-system is unavailable")
            false
        } else {
            standardOutput.toString().contains("--build-system")
        }
    }
