package io.github.frankois944.spmForKmp

import com.autonomousapps.kit.GradleBuilder
import io.github.frankois944.spmForKmp.fixture.SmpKMPTestFixture
import io.github.frankois944.spmForKmp.fixture.SwiftSource
import io.github.frankois944.spmForKmp.utils.BaseTest
import io.github.frankois944.spmForKmp.utils.SdkDeploymentFloors
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test

/**
 * The `swiftbuild` build system validates the deployment target against the range the installed
 * SDK supports and fails the build when it is lower; the deprecated `native` one did not.
 *
 * The plugin warns while it still builds with `native`, so a project that would break on the new
 * build system is visible before the switch.
 *
 * Every version here is derived from the installed SDK rather than written down: the floor moves
 * with Xcode — 15.0 for iOS on Xcode 27, low enough to accept 12.0 on Xcode 26 — so a hardcoded
 * version makes the test pass or fail on the toolchain rather than on the plugin.
 */
class DeploymentTargetTest : BaseTest() {
    private val manifestTask = ":library:SwiftPackageConfigAppleDummyGenerateSwiftPackage"

    private fun runWith(
        minIos: String,
        minWatchos: String? = null,
    ) = GradleBuilder
        .runner(
            SmpKMPTestFixture
                .builder()
                .withBuildPath(testProjectDir.root.absolutePath)
                .withMinIos(minIos)
                .apply { minWatchos?.let { withMinWatchOs(it) } }
                .withSwiftSources(
                    SwiftSource.of(
                        content =
                            """
                            import Foundation
                            @objc public class TestValue: NSObject {}
                            """.trimIndent(),
                    ),
                ).build()
                .gradleProject.rootDir,
            manifestTask,
        ).build()
        .output

    @Test
    fun `warns when the deployment target is below what the SDK supports`() {
        val floor = SdkDeploymentFloors.lowestSupported("iphoneos")
        assumeTrue(floor != null, "could not read what the iphoneos SDK supports")
        val tooLow = SdkDeploymentFloors.justBelow(floor!!)
        assumeTrue(tooLow != null, "the iphoneos SDK has no deployment target below $floor")

        val output = runWith(minIos = tooLow!!)

        assertTrue(
            output.contains("swiftPackageConfig.minIos is $tooLow"),
            "expected a warning about minIos $tooLow being below $floor, got:\n$output",
        )
    }

    @Test
    fun `does not warn when the deployment target is supported`() {
        val floor = SdkDeploymentFloors.lowestSupported("iphoneos")
        assumeTrue(floor != null, "could not read what the iphoneos SDK supports")

        val output = runWith(minIos = floor!!)

        assertFalse(
            output.contains("swiftPackageConfig.minIos"),
            "expected no warning for minIos $floor, got:\n$output",
        )
    }

    /**
     * The manifest declares every platform, but the fixture only targets iOS. A watchOS
     * deployment target below what the watchOS SDK supports must not be reported for a project
     * that builds nothing for watchOS.
     */
    @Test
    fun `does not warn about a platform the project does not target`() {
        val iosFloor = SdkDeploymentFloors.lowestSupported("iphoneos")
        val watchosFloor = SdkDeploymentFloors.lowestSupported("watchos")
        assumeTrue(iosFloor != null && watchosFloor != null, "could not read what the SDKs support")
        val tooLowForWatchos = SdkDeploymentFloors.justBelow(watchosFloor!!)
        assumeTrue(tooLowForWatchos != null, "the watchos SDK has no deployment target below $watchosFloor")

        val output = runWith(minIos = iosFloor!!, minWatchos = tooLowForWatchos!!)

        assertFalse(
            output.contains("swiftPackageConfig.minWatchos"),
            "expected no watchOS warning for an iOS-only project, got:\n$output",
        )
    }
}
