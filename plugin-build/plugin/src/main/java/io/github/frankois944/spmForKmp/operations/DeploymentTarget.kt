package io.github.frankois944.spmForKmp.operations

import org.gradle.api.logging.Logger
import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream
import java.util.concurrent.ConcurrentHashMap

/**
 * Compares two Apple deployment target versions, component by component.
 *
 * The versions are dotted decimals of an arbitrary length — `12.0`, `10.13`, `26.0` — so a
 * lexicographic comparison gets `10.13` and `10.9` the wrong way round.
 * A component that cannot be read as a number sorts as 0, which keeps a malformed value from
 * failing the build: the SDK itself has the final say.
 */
internal fun compareDeploymentVersions(
    left: String,
    right: String,
): Int {
    val leftParts = left.trim().split(".")
    val rightParts = right.trim().split(".")
    for (index in 0 until maxOf(leftParts.size, rightParts.size)) {
        val leftPart = leftParts.getOrNull(index)?.toIntOrNull() ?: 0
        val rightPart = rightParts.getOrNull(index)?.toIntOrNull() ?: 0
        if (leftPart != rightPart) return leftPart.compareTo(rightPart)
    }
    return 0
}

private val versionRegex = """"(\d+(?:\.\d+)*)"""".toRegex()

/**
 * The lowest deployment target the installed SDK still accepts, or `null` when it cannot be read.
 *
 * `swiftbuild` validates the deployment target against this range and fails the build when it is
 * lower; the deprecated `native` build system did not. Reading it from the SDK rather than
 * hardcoding it keeps the check correct across Xcode releases.
 *
 * The result is memoized per SDK: the probe spawns two processes and would otherwise run once
 * per package.
 */
private val sdkDeploymentFloors = ConcurrentHashMap<String, String>()

internal fun ExecOperations.getSdkDeploymentFloor(
    sdk: String,
    logger: Logger,
): String? =
    sdkDeploymentFloors
        .computeIfAbsent(sdk) { name ->
            val sdkPath = readSdkPath(name, logger)
            if (sdkPath.isEmpty()) {
                ""
            } else {
                readSupportedVersions("$sdkPath/SDKSettings.plist", logger)
                    .minWithOrNull(::compareDeploymentVersions)
                    .orEmpty()
            }
        }.takeIf { it.isNotEmpty() }

private fun ExecOperations.readSdkPath(
    sdk: String,
    logger: Logger,
): String {
    val standardOutput = ByteArrayOutputStream()
    val errorOutput = ByteArrayOutputStream()
    val result =
        exec {
            it.executable = "xcrun"
            it.args = listOf("--sdk", sdk, "--show-sdk-path")
            it.standardOutput = standardOutput
            it.errorOutput = errorOutput
            it.isIgnoreExitValue = true
        }
    if (result.exitValue != 0) {
        logger.info("Could not locate the $sdk SDK, skipping the deployment target check")
        return ""
    }
    return standardOutput.toString().trim()
}

private fun ExecOperations.readSupportedVersions(
    plistPath: String,
    logger: Logger,
): List<String> {
    val standardOutput = ByteArrayOutputStream()
    val errorOutput = ByteArrayOutputStream()
    val result =
        exec {
            it.executable = "plutil"
            it.args =
                listOf(
                    "-extract",
                    "DefaultProperties.DEPLOYMENT_TARGET_SUGGESTED_VALUES",
                    "json",
                    "-o",
                    "-",
                    plistPath,
                )
            it.standardOutput = standardOutput
            it.errorOutput = errorOutput
            it.isIgnoreExitValue = true
        }
    if (result.exitValue != 0) {
        logger.info("Could not read the supported deployment targets from $plistPath")
        return emptyList()
    }
    return versionRegex
        .findAll(standardOutput.toString())
        .map { it.groupValues[1] }
        .toList()
}
