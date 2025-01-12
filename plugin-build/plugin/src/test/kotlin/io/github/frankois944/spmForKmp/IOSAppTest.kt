package io.github.frankois944.spmForKmp

import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IOSAppTest {
    @Suppress("MaxLineLength")
    /*
     * Run the command xcodebuild -project iosApp.xcodeproj -scheme iosApp -configuration Debug -destination 'generic/platform=iOS Simulator' ARCHS=arm64 -derivedDataPath "./build" -clonedSourcePackagesDirPath "./spm" build
     * This should also do a simple test as proof it's starting, but I think it will break the CI quota
     */
    @Test
    fun `build the ios example app`() {
        val projectRoot = System.getProperty("user.dir") + "/../../"
        val workingDir = File("$projectRoot/example/iosApp")
        val derivedDataPath = File("$projectRoot/example/iosApp/build")
        val clonedSourcePackagesDirPath = File("$projectRoot/example/iosApp/spm")
        val command =
            listOf(
                "xcodebuild",
                "-project",
                "iosApp.xcodeproj",
                "-scheme",
                "iosApp",
                "-configuration",
                "Debug",
                "-destination",
                "generic/platform=iOS Simulator",
                "ARCHS=arm64",
                "-derivedDataPath",
                derivedDataPath.path,
                "-clonedSourcePackagesDirPath",
                clonedSourcePackagesDirPath.path,
                "build",
            )

        // Set up the ProcessBuilder
        val processBuilder = ProcessBuilder(command)
        processBuilder.directory(workingDir)
        processBuilder.redirectErrorStream(true) // Redirect stderr to stdout
        processBuilder.redirectOutput(ProcessBuilder.Redirect.PIPE)

        // Start the process and wait for it to complete
        val process = processBuilder.start()
        val output = process.inputStream.bufferedReader().readText()
        val exitCode = process.waitFor()

        // Assert on the process exit code or the output
        println("Process output:\n$output")
        assertEquals(0, exitCode, "The process should exit successfully.")
        assertTrue(output.contains("BUILD SUCCEEDED"), "Build output should indicate success.")
    }
}
