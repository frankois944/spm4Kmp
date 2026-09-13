package io.github.frankois944.spmForKmp

import io.github.frankois944.spmForKmp.utils.BaseTest
import org.junit.jupiter.api.Assumptions.assumeFalse
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class IOSAppTest : BaseTest() {
    /**
     * The identifier of an iPhone simulator this machine can actually boot, or `null` when it
     * has none — or no Xcode toolchain at all to ask.
     */
    private fun findSimulatorId(): String? =
        runCatching {
            val process =
                ProcessBuilder("xcrun", "simctl", "list", "devices", "available")
                    .redirectErrorStream(true)
                    .start()

            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()

            println(output)

            findUsableIPhone(output).also {
                println("found id:$it")
            }
        }.getOrNull()

    /**
     * `simctl` groups the devices by runtime and still lists those of a runtime that is not
     * installed, under an `-- Unavailable: … --` header. Picking one of those gives `xcodebuild`
     * a destination it cannot match, so only the devices of an installed runtime are considered.
     */
    private fun findUsableIPhone(devices: String): String? {
        var usableRuntime = false
        devices.lineSequence().forEach { line ->
            val header = RUNTIME_HEADER.find(line.trim())
            if (header != null) {
                usableRuntime = !header.groupValues[1].startsWith("Unavailable", ignoreCase = true)
                return@forEach
            }
            if (usableRuntime && !line.contains("unavailable", ignoreCase = true)) {
                SIMULATOR_ID.find(line)?.let { return it.groupValues[1] }
            }
        }
        return null
    }

    @Test
    fun `build and test example app`() {
        if (System.getenv("GITEA_TOKEN").isNullOrEmpty()) {
            println("SKIP TEST because no GITEA_TOKEN set")
            return
        }

        // Nothing to run the app on: report the test as skipped rather than failing a machine
        // that has no simulator installed.
        val simulatorId = findSimulatorId()
        assumeTrue(simulatorId != null) {
            "SKIP TEST because no iPhone simulator is available"
        }

        val xcodeBuildCommand =
            listOf(
                "xcodebuild",
                "-project",
                "iosApp.xcodeproj",
                "-scheme",
                "iosApp",
                "-configuration",
                "Debug",
                "-destination",
                "id=$simulatorId",
                "-derivedDataPath",
                "./build",
                "-clonedSourcePackagesDirPath",
                "./spm",
                "-testPlan",
                "iosApp",
                "clean",
                "test",
            )

        val workDir = File("../../example/iosApp")

        val xcodebuild =
            ProcessBuilder(xcodeBuildCommand)
                .directory(workDir)
                .redirectErrorStream(true)
                .start()

        val xcbeautifyCmd =
            if (isCI) {
                listOf(
                    "xcbeautify",
                    "--disable-logging",
                    "--preserve-unbeautified",
                    "--report",
                    "junit",
                )
            } else {
                listOf(
                    "xcbeautify",
                    "--disable-logging",
                    "--preserve-unbeautified",
                    "--report",
                    "junit",
                )
            }

        val xcbeautify =
            ProcessBuilder(xcbeautifyCmd)
                .directory(workDir)
                .redirectErrorStream(true)
                .start()

        // Pipe xcodebuild -> xcbeautify
        val ioPool = Executors.newFixedThreadPool(2)
        val pipeFuture =
            ioPool.submit {
                xcodebuild.inputStream.use { src ->
                    xcbeautify.outputStream.use { sink ->
                        src.copyTo(sink)
                        // use{} closes sink to signal EOF to xcbeautify
                    }
                }
            }

        // Capture xcbeautify output concurrently
        val outputSb = StringBuilder()
        val readFuture =
            ioPool.submit {
                xcbeautify.inputStream.bufferedReader().useLines { lines ->
                    lines.forEach { line ->
                        outputSb.appendLine(line)
                    }
                }
            }

        // Wait for piping to complete before joining processes
        pipeFuture.get(20, TimeUnit.MINUTES)
        readFuture.get(20, TimeUnit.MINUTES)
        ioPool.shutdown()

        val xcodebuildFinished = xcodebuild.waitFor(20, TimeUnit.MINUTES)
        val xcbeautifyFinished = xcbeautify.waitFor(10, TimeUnit.MINUTES)

        val xcodebuildExit = if (xcodebuildFinished) xcodebuild.exitValue() else -1
        val xcbeautifyExit = if (xcbeautifyFinished) xcbeautify.exitValue() else -1

        val finalOutput = outputSb.toString()

        // The simulator listed by `simctl` can still be unusable by `xcodebuild`, typically when
        // its runtime is not installed on the machine. Nothing was tested, but nothing is broken
        // either: report the test as skipped instead of failing the build of a runner without a
        // usable simulator.
        assumeFalse(finalOutput.hasNoUsableDestination()) {
            "SKIP TEST because no usable iOS simulator was found:\n$finalOutput"
        }

        // Emulate `set -o pipefail`: fail if either process failed
        assert(xcodebuildExit == 0 && xcbeautifyExit == 0) {
            buildString {
                appendLine("Pipeline failed:")
                appendLine("xcodebuild exit=$xcodebuildExit, xcbeautify exit=$xcbeautifyExit")
                appendLine("Output:")
                append(finalOutput)
            }
        }

//        if (!isCI && xcodebuildExit == 0 && xcbeautifyExit == 0) {
        //          println(finalOutput)
        //      }
    }

    /**
     * Whether `xcodebuild` refused to run because the machine has nothing to run the app on,
     * as opposed to a real failure of the build or of the tests.
     */
    private fun String.hasNoUsableDestination(): Boolean =
        NO_DESTINATION_ERRORS.any { contains(it, ignoreCase = true) }

    private companion object {
        val SIMULATOR_ID = Regex("""iPhone.*\(([-A-F0-9]+)\)""")
        val RUNTIME_HEADER = Regex("""^-- (.+) --$""")

        val NO_DESTINATION_ERRORS =
            listOf(
                "Unable to find a destination matching",
                "Unable to find a device matching",
                "Please download and install the platform",
                "Unsupported destination",
            )
    }
}
