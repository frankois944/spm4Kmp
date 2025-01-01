package io.frankois944.spmForKmp.plugin.operations

import io.frankois944.spmForKmp.plugin.CompileTarget
import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream
import java.io.File

internal fun ExecOperations.resolvePackage(
    workingDir: File,
    scratchPath: File,
) {
    val args =
        listOf(
            "swift",
            "package",
            "resolve",
            "--scratch-path",
            scratchPath.path,
        )

    val output = ByteArrayOutputStream()
    exec {
        it.executable = "xcrun"
        it.args = args
        it.workingDir = workingDir
        it.standardOutput = output
    }
}

internal fun ExecOperations.getXcodeVersion(): String {
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
    val regex = """Xcode\s(\d+\.\d+)""".toRegex()
    val match = regex.find(output.toString())
    return match?.groups?.get(1)?.value ?: throw RuntimeException("Can't find Xcode version")
}

internal fun ExecOperations.getXcodeDevPath(): String {
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
    return output.toString().trim()
}

internal fun ExecOperations.getSDKPath(target: CompileTarget): String {
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
    return output.toString().trim()
}
