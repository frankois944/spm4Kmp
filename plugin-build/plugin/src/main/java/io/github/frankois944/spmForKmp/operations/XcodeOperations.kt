package io.github.frankois944.spmForKmp.operations

import io.github.frankois944.spmForKmp.CompileTarget
import org.gradle.api.logging.Logger
import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream
import java.io.File

internal fun ExecOperations.resolvePackage(
    workingDir: File,
    scratchPath: File,
    sharedCachePath: File?,
    sharedConfigPath: File?,
    sharedSecurityPath: File?,
) {
    val args =
        mutableListOf(
            "swift",
            "package",
            "resolve",
            "--scratch-path",
            scratchPath.path,
        ).also { list ->
            sharedCachePath?.let {
                list.add("--cache-path")
                list.add(it.path)
            }
            sharedConfigPath?.let {
                list.add("--config-path")
                list.add(it.path)
            }
            sharedSecurityPath?.let {
                list.add("--security-path")
                list.add(it.path)
            }
        }

    val output = ByteArrayOutputStream()
    exec {
        it.executable = "xcrun"
        it.args = args
        it.workingDir = workingDir
        it.standardOutput = output
    }
}

internal fun ExecOperations.getXcodeVersion(logger: Logger? = null): String {
    val args =
        listOf(
            "xcodebuild",
            "-version",
        )
    val output = ByteArrayOutputStream()
    exec {
        it.executable = "xcrun"
        it.args = args
        it.standardOutput = output
    }
    logger?.debug(
        """
RUN getXcodeVersion
ARGS xcrun ${args.joinToString(" ")}
OUTPUT $output
        """.trimMargin(),
    )
    val regex = """Xcode\s(\d+\.\d+)""".toRegex()
    val match = regex.find(output.toString())
    return match?.groups?.get(1)?.value ?: throw RuntimeException("Can't find Xcode version")
}

internal fun ExecOperations.getXcodeDevPath(logger: Logger? = null): String {
    val args =
        listOf(
            "xcode-select",
            "-p",
        )

    val output = ByteArrayOutputStream()
    exec {
        it.executable = "xcrun"
        it.args = args
        it.standardOutput = output
    }
    logger?.debug(
        """
RUN getXcodeVersion
ARGS xcrun ${args.joinToString(" ")}
OUTPUT $output
        """.trimMargin(),
    )
    return output.toString().trim()
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

    val output = ByteArrayOutputStream()
    exec {
        it.executable = "xcrun"
        it.args = args
        it.standardOutput = output
    }
    logger?.debug(
        """
RUN getSDKPath
ARGS xcrun ${args.joinToString(" ")}
OUTPUT $output
        """.trimMargin(),
    )
    return output.toString().trim()
}
