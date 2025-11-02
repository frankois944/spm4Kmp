package io.github.frankois944.spmForKmp

import io.github.frankois944.spmForKmp.utils.BaseTest
import org.junit.jupiter.api.Test
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class IOSAppTest : BaseTest() {
    @Test
    fun `build and test example app`() {
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
                "platform=iOS Simulator,name=iPhone SE (3rd generation)",
                "-derivedDataPath",
                "./build",
                "-clonedSourcePackagesDirPath",
                "./spm",
                "-testPlan",
                "iosApp",
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

        // Emulate `set -o pipefail`: fail if either process failed
        val finalOutput = outputSb.toString()
        assert(xcodebuildExit == 0 && xcbeautifyExit == 0) {
            buildString {
                appendLine("Pipeline failed:")
                appendLine("xcodebuild exit=$xcodebuildExit, xcbeautify exit=$xcbeautifyExit")
                appendLine("Output:")
                append(finalOutput)
            }
        }

        if (!isCI && xcodebuildExit == 0 && xcbeautifyExit == 0) {
            println(finalOutput)
        }
    }
}
