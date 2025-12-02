package io.github.frankois944.spmForKmp.operations

import org.gradle.api.logging.Logger
import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.URI

internal fun ExecOperations.packageRegistrySet(
    workingDir: File,
    url: URI,
    logger: Logger,
    swiftBinPath: String?,
) {
    // swift package-registry set
    // [--global]
    // [--scope <scope>]
    // [--allow-insecure-http]
    // <url>
    val args =
        buildList {
            if (swiftBinPath == null) {
                add("--sdk")
                add("macosx")
                add("swift")
            }
            add("package-registry")
            add("set")
            add("--allow-insecure-http")
            add(url.toString())
        }

    logger.debug("Executing package registry set command")
    logger.debug("Package registry set command args: ${args.joinToString(" ")}")
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
            "packageRegistrySet",
            args,
            it.exitValue != 0,
            standardOutput,
            errorOutput,
        )
    }
}

internal fun ExecOperations.packageRegistryAuth(
    workingDir: File,
    url: URI,
    username: String?,
    password: String?,
    token: String?,
    tokenFile: File?,
    logger: Logger,
    swiftBinPath: String?,
) {
    // swift package-registry login
    // [<url>]
    // [--username <username>]
    // [--password <password>]
    // [--token <token>]
    // [--token-file <token-file>]
    // [--no-confirm]
    val args =
        buildList {
            if (swiftBinPath == null) {
                add("--sdk")
                add("macosx")
                add("swift")
            }
            add("package-registry")
            add("login")
            add(url.toString())
            username?.let {
                add("--username")
                add(it)
            }
            password?.let {
                add("--password")
                add(it)
            }
            token?.let {
                add("--token")
                add(it)
            }
            tokenFile?.let {
                add("--token-file")
                add(it.toString())
            }
            add("--no-confirm")
        }

    logger.debug("Executing package registry login command")
    logger.debug("Package registry login command args: ${args.joinToString(" ")}")
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
            "packageRegistryAuth",
            args,
            it.exitValue != 0,
            standardOutput,
            errorOutput,
        )
    }
}
