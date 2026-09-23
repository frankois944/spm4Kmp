package io.github.frankois944.spmForKmp

import com.autonomousapps.kit.GradleBuilder
import io.github.frankois944.spmForKmp.fixture.SmpKMPTestFixture
import io.github.frankois944.spmForKmp.fixture.SwiftSource
import io.github.frankois944.spmForKmp.utils.BaseTest
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * The `swiftbuild` build system validates the deployment target against the range the installed
 * SDK supports and fails the build when it is lower; the deprecated `native` one did not.
 *
 * The plugin warns while it still builds with `native`, so a project that would break on the new
 * build system is visible before the switch.
 */
class DeploymentTargetTest : BaseTest() {
    private val manifestTask = ":library:SwiftPackageConfigAppleDummyGenerateSwiftPackage"

    private fun runWithMinIos(minIos: String) =
        GradleBuilder
            .runner(
                SmpKMPTestFixture
                    .builder()
                    .withBuildPath(testProjectDir.root.absolutePath)
                    .withMinIos(minIos)
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
        val output = runWithMinIos("12.0")

        assertTrue(
            output.contains("swiftPackageConfig.minIos is 12.0"),
            "expected a deployment target warning, got:\n$output",
        )
    }

    /**
     * The manifest declares every platform, but the fixture only targets iOS. Its watchOS
     * deployment target (4.0) is below what the watchOS SDK supports (9.0 on Xcode 27), and
     * must not be reported for a project that builds nothing for watchOS.
     */
    @Test
    fun `does not warn about a platform the project does not target`() {
        val output = runWithMinIos("15.0")

        assertFalse(
            output.contains("swiftPackageConfig.minWatchos"),
            "expected no watchOS warning for an iOS-only project, got:\n$output",
        )
    }

    @Test
    fun `does not warn when the deployment target is supported`() {
        val output = runWithMinIos("15.0")

        assertFalse(
            output.contains("swiftPackageConfig.minIos"),
            "expected no deployment target warning, got:\n$output",
        )
    }
}
