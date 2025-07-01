package io.github.frankois944.spmForKmp

import org.junit.jupiter.api.Test
import java.io.File
import java.util.concurrent.TimeUnit

class IOSAppTest {
    @Test
    fun `build and test example app`() {
        val xcodebuild =
            ProcessBuilder()
                .command(
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
                    "clean",
                    "test",
                ).directory(File("../../example/iosApp"))
                .start()

        // Create xcpretty process with output capture
        val xcpretty =
            (
                if (System.getenv("GITHUB_ACTIONS") == "true") {
                    ProcessBuilder(
                        "xcbeautify",
                        "--renderer",
                        "github-actions",
                    )
                } else {
                    ProcessBuilder(
                        "xcbeautify",
                    )
                }
            ).redirectErrorStream(true)
                .start()

        // Pipe xcodebuild output to xcpretty
        xcodebuild.inputStream.copyTo(xcpretty.outputStream)
        xcpretty.outputStream.close()

        // Capture and print xcpretty output
        val finalOutput =
            buildString {
                xcpretty.inputStream.bufferedReader().forEachLine { line ->
                    appendLine(line)
                }
            }

        // Wait for both processes to complete
        val xcodeBuildExitCode = xcodebuild.waitFor(30, TimeUnit.MINUTES)
        val xcprettyExitCode = xcpretty.waitFor(1, TimeUnit.MINUTES)

        // Check exit codes and output
        assert(xcodebuild.exitValue() == 0) {
            "xcodebuild process failed with exit code ${xcodebuild.exitValue()}\nOutput:\n$finalOutput"
        }
        if (xcodebuild.exitValue() == 0 && System.getenv("GITHUB_ACTIONS") == "true") {
            println(finalOutput)
        }
    }
}
